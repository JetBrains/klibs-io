package io.klibs.core.search.eval

import io.awspring.cloud.s3.S3Template
import io.klibs.app.Application
import io.klibs.core.search.opensearch.IndexNaming
import io.klibs.core.search.opensearch.OpenSearchIndexer
import io.klibs.integration.ai.AiService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc

/**
 * Search-eval EVAL tier (KTL-4710): the aspirational upper bound.
 * Run:
 * ```
 * ./kotlin test -m app --include-classes '*SearchEvalE2ETest' --jvm-args '-Dsearch.eval.tier=eval'
 * ```
 * Runs every case against a live **prod-copy** DB (`SEARCH_EVAL_DB_*` env; defaults to a local `klibs` DB).
 * Each run diffs against the previous one recorded in `build/search-eval/last-run.json`: run it once
 * before a search change and once after, and the delta is the change rather than corpus drift.
 */
@EnabledIfSystemProperty(named = "search.eval.tier", matches = "eval")
@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = [
    OpenAiChatAutoConfiguration::class,
    OpenAiAudioTranscriptionAutoConfiguration::class,
    OpenAiAudioSpeechAutoConfiguration::class,
    OpenAiEmbeddingAutoConfiguration::class,
    OpenAiImageAutoConfiguration::class,
    OpenAiModerationAutoConfiguration::class,
])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchEvalE2ETest : SearchEvalTestBase() {

    @MockitoBean
    private lateinit var aiService: AiService

    @MockitoBean
    private lateinit var s3Template: S3Template

    @Autowired
    override lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var indexer: OpenSearchIndexer

    @Autowired
    private lateinit var naming: IndexNaming

    @Autowired
    private lateinit var openSearchClient: OpenSearchClient

    override val tier = "eval"

    /** The eval tier is aspirational: every case runs and every case is expected to pass. */
    override fun casesToRun() = SearchEvalData.loadCases()

    /**
     * Headline delta and which cases gained or lost ground since the previous run on this machine,
     * then record this run as the next "before". Run once before a search change and once after —
     * both against the same prod-copy, so the delta is the change and nothing else.
     */
    override fun onRunComplete(report: RunReport) {
        val passing = report.passingIds().toSet()
        val previous = SearchEvalData.loadLastRun()
        if (previous == null) {
            log.info("no previous eval run recorded — this run is the 'before'. Re-run after the change to see the delta.")
        } else {
            log.info(
                "eval vs previous run: headline {} -> {} ({})  gained={}  lost={}",
                "%.4f".format(previous.headline), "%.4f".format(report.headline),
                "%+.4f".format(report.headline - previous.headline),
                (passing - previous.passing.toSet()).sorted(), (previous.passing.toSet() - passing).sorted(),
            )
        }
        SearchEvalData.writeLastRun(EvalRunRecord(report.headline, passing.sorted()))
    }

    @BeforeAll
    fun refreshViews() {
        naming.all.forEach { indexer.sync(it) }
        // Count through the alias, and top-level: `_cat` docs.count inflates with nested `packages`.
        val alias = naming.project.alias
        val indexed = openSearchClient.count { it.index(alias) }.count()
        check(indexed > 0) {
            "OpenSearch project alias '$alias' is empty after sync — " +
                "is OpenSearch up at $OS_URI and the prod-copy DB populated?"
        }
        log.info("OpenSearch project alias '{}' has {} docs", alias, indexed)
    }

    companion object {
        private fun env(key: String, default: String) = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

        private val OS_URI = env("SEARCH_EVAL_OS_URI", "https://localhost:9200")
        private val OS_PROJECT_INDEX = env("SEARCH_EVAL_OS_PROJECT_INDEX", "project-eval")
        private val OS_PACKAGE_INDEX = env("SEARCH_EVAL_OS_PACKAGE_INDEX", "package-eval")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { env("SEARCH_EVAL_DB_URL", "jdbc:postgresql://localhost:5432/klibs") }
            registry.add("spring.datasource.username") { env("SEARCH_EVAL_DB_USER", "klibs") }
            registry.add("spring.datasource.password") { env("SEARCH_EVAL_DB_PASSWORD", "klibs") }
            // Drive the production search path through OpenSearch (eval-specific indices, wiped+refilled).
            registry.add("klibs.search.opensearch.enabled") { "true" }
            registry.add("klibs.search.opensearch.uri") { OS_URI }
            registry.add("klibs.search.opensearch.trust-all-certificates") { "true" }
            registry.add("klibs.search.opensearch.username") { "admin" }
            registry.add("klibs.search.opensearch.password") { "OpenSearch!ocalPassw0rd" }
            registry.add("klibs.search.opensearch.project-index") { OS_PROJECT_INDEX }
            registry.add("klibs.search.opensearch.package-index") { OS_PACKAGE_INDEX }
            // Corpus is a prod-copy; never seed the `test` profile's data.sql fixtures.
            registry.add("spring.sql.init.mode") { "never" }
            registry.add("klibs.readme.s3.bucket-name") { "test-bucket" }
            registry.add("klibs.readme.s3.prefix") { "readme" }
            registry.add("klibs.integration.github.cache.request-cache-path") { "build/tmp/gh-req-cache" }
        }
    }
}
