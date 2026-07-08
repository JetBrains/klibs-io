package io.klibs.app.eval

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.service.SearchService
import io.klibs.integration.ai.AiService
import io.klibs.integration.ai.EmbedderRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

/**
 * E0 + E1 evaluation runner. Enabled only with `-Dklibs.eval.enabled=true` so it never runs during
 * a normal boot. For the top-frequency concept queries it ranks the FTS baseline and every embedding
 * column, pools the top-k results, grades the pool with the cached LLM judge, then writes a Markdown
 * report of nDCG@10 / MRR / Recall@10 / Recall@20 and latency per engine. Requires a populated
 * database (and an OpenAI key for the OpenAI columns); the `local` column works offline.
 */
@Component
@ConditionalOnProperty("klibs.eval.enabled", havingValue = "true")
class EmbeddingEvaluationRunner(
    private val searchService: SearchService,
    private val embedderRegistry: EmbedderRegistry,
    private val queryLoader: EvaluationQueryLoader,
    private val relevanceJudge: RelevanceJudge,
    private val objectMapper: ObjectMapper,
    @Value("\${klibs.eval.query-limit:150}") private val queryLimit: Int,
    @Value("\${klibs.eval.pool-k:10}") private val poolK: Int,
    @Value("\${klibs.eval.judgments-file:eval-output/judgments.json}") private val judgmentsFile: String,
    @Value("\${klibs.eval.report-file:eval-output/embedding-eval-report.md}") private val reportFile: String,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) = evaluate()

    fun evaluate() {
        val queries = queryLoader.load(queryLimit)
        val engines = buildEngines()
        val retrievalLimit = maxOf(20, poolK)
        val store = JudgmentStore(Paths.get(judgmentsFile), objectMapper).apply { load() }
        val metricsByEngine = engines.associate { it.engineName to mutableListOf<QueryMetrics>() }
        var llmJudgeCalls = 0

        queries.forEachIndexed { index, wq ->
            val rankings = engines.associateWith { rankSafely(it, wq.text, retrievalLimit) }
            val pool = poolCandidates(rankings.values)
            val judgments = store.get(wq.text) ?: relevanceJudge.judge(wq.text, pool).also {
                store.put(wq.text, it); store.save(); llmJudgeCalls++
            }
            val relevantIds = judgments.filterValues { it > 0 }.keys
            engines.forEach { engine ->
                metricsByEngine.getValue(engine.engineName).add(toMetrics(rankings.getValue(engine), judgments, relevantIds))
            }
            logger.info("Evaluated ${index + 1}/${queries.size} '${wq.text}' (pool=${pool.size})")
        }

        writeReport(engines, metricsByEngine, queries.size, llmJudgeCalls)
    }

    private fun buildEngines(): List<Ranker> =
        listOf(FtsRanker(searchService)) + embedderRegistry.all.map { SemanticRanker(searchService, it.embedderName) }

    private fun rankSafely(engine: Ranker, query: String, limit: Int): Ranking {
        var results: List<SearchProjectResult> = emptyList()
        val ms = measureTimeMillis {
            results = runCatching { engine.rank(query, limit) }
                .onFailure { logger.warn("Engine '${engine.engineName}' failed on '$query': ${it.message}") }
                .getOrDefault(emptyList())
        }
        return Ranking(results, ms)
    }

    private fun poolCandidates(rankings: Collection<Ranking>): List<SearchProjectResult> {
        val seen = LinkedHashMap<Int, SearchProjectResult>()
        rankings.forEach { ranking -> ranking.results.take(poolK).forEach { seen.putIfAbsent(it.id, it) } }
        return seen.values.toList()
    }

    private fun toMetrics(ranking: Ranking, judgments: Map<Int, Int>, relevantIds: Set<Int>): QueryMetrics {
        val ids = ranking.results.map { it.id }
        return QueryMetrics(
            ndcg10 = RankingMetrics.ndcgAtK(ids, judgments, 10),
            mrr = RankingMetrics.reciprocalRank(ids, judgments),
            recall10 = RankingMetrics.recallAtK(ids, relevantIds, 10),
            recall20 = RankingMetrics.recallAtK(ids, relevantIds, 20),
            latencyMillis = ranking.latencyMillis,
        )
    }

    private fun writeReport(
        engines: List<Ranker>,
        metricsByEngine: Map<String, List<QueryMetrics>>,
        queryCount: Int,
        llmJudgeCalls: Int,
    ) {
        val reports = engines.map { EvaluationReport.aggregate(it.engineName, metricsByEngine.getValue(it.engineName)) }
        val markdown = EvaluationReport.renderMarkdown(reports, buildNotes(engines, queryCount, llmJudgeCalls))
        val path = Paths.get(reportFile)
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, markdown)
        logger.info("Wrote evaluation report to ${path.toAbsolutePath()}\n$markdown")
    }

    private fun buildNotes(engines: List<Ranker>, queryCount: Int, llmJudgeCalls: Int): String = buildString {
        val openAi = engines.map { it.engineName }.filter { it.startsWith("openai") }
        appendLine("- queries: $queryCount (top-$queryLimit by frequency), pool k=$poolK")
        appendLine("- LLM judge calls (uncached): $llmJudgeCalls (model ${AiService.DEFAULT_GPT})")
        appendLine("- query embeddings per OpenAI engine: $queryCount each (${openAi.joinToString()})")
        appendLine("- reference embedding price /1M tokens: 3-small \$0.02, ada-002 \$0.10, 3-large \$0.13, local free")
        appendLine("- token-level OpenAI cost is captured by the existing `klibs.*` OpenAI metrics.")
    }

    private data class Ranking(val results: List<SearchProjectResult>, val latencyMillis: Long)

    private companion object {
        private val logger = LoggerFactory.getLogger(EmbeddingEvaluationRunner::class.java)
    }
}
