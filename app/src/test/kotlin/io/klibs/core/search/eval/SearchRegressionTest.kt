package io.klibs.core.search.eval

import io.awspring.cloud.s3.S3Template
import io.klibs.app.Application
import io.klibs.core.search.service.SearchService
import io.klibs.integration.ai.AiService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc

/**
 * Search-eval REGRESSION tier (KTL-4710): the lower bound.
 *
 * Manual (needs Docker + a frozen snapshot).
 * Run with:
 * `./kotlin test -m app --include-classes '*SearchRegressionTest' --jvm-args '-Dsearch.eval.tier=regression'`.
**/
@EnabledIfSystemProperty(named = "search.eval.tier", matches = "regression")
@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@Import(FrozenSnapshotPostgresConfig::class)
@EnableAutoConfiguration(exclude = [
    OpenAiChatAutoConfiguration::class,
    OpenAiAudioTranscriptionAutoConfiguration::class,
    OpenAiAudioSpeechAutoConfiguration::class,
    OpenAiEmbeddingAutoConfiguration::class,
    OpenAiImageAutoConfiguration::class,
    OpenAiModerationAutoConfiguration::class,
])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchRegressionTest : SearchEvalTestBase() {

    @MockitoBean
    private lateinit var aiService: AiService

    @MockitoBean
    private lateinit var s3Template: S3Template

    @Autowired
    override lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var searchService: SearchService

    override val tier = "regression"
    override val isRecording get() = overwriteFloor

    /**
     * The floor is the gate, so a normal run only executes floor ids.
     * An overwrite run executes everything to recompute the new floor.
     */
    override fun casesToRun(): List<EvalCase> {
        val cases = SearchEvalData.loadCases()
        if (overwriteFloor) return cases
        val byId = cases.associateBy { it.id }
        return SearchEvalData.loadFloor().map { id -> byId[id] ?: error("floor id '$id' not in answer key") }
    }

    /** `-Dsearch.floor.overwrite`: record the ids passing on the frozen snapshot as the new floor. */
    override fun onRunComplete(report: RunReport) {
        if (!overwriteFloor) return
        val passing = report.passingIds()
        SearchEvalData.writeFloor(passing)
        log.info("floor.json rewritten: {} ids", passing.size)
    }

    @BeforeAll
    fun refreshViews() = searchService.refreshSearchViews()

    private val overwriteFloor get() = System.getProperty("search.floor.overwrite") != null

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // Datasource is wired from the @ServiceConnection container; only app-level props here.
            registry.add("spring.sql.init.mode") { "never" }
            registry.add("klibs.readme.s3.bucket-name") { "test-bucket" }
            registry.add("klibs.readme.s3.prefix") { "readme" }
            registry.add("klibs.integration.github.cache.request-cache-path") { "build/tmp/gh-req-cache" }
        }
    }
}
