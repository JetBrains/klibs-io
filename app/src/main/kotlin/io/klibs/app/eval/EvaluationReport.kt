package io.klibs.app.eval

import kotlin.math.ceil

/** Metrics for one engine on a single query. */
data class QueryMetrics(
    val ndcg10: Double,
    val mrr: Double,
    val recall10: Double,
    val recall20: Double,
    val latencyMillis: Long,
)

/** Aggregated metrics for one engine across the whole query set. */
data class EngineReport(
    val engineName: String,
    val queries: Int,
    val meanNdcg10: Double,
    val mrr: Double,
    val meanRecall10: Double,
    val meanRecall20: Double,
    val p50LatencyMillis: Long,
    val p95LatencyMillis: Long,
)

/** Pure aggregation of per-query metrics into per-engine reports and a Markdown table. */
object EvaluationReport {

    fun aggregate(engineName: String, perQuery: List<QueryMetrics>): EngineReport {
        if (perQuery.isEmpty()) return EngineReport(engineName, 0, 0.0, 0.0, 0.0, 0.0, 0, 0)
        val latencies = perQuery.map { it.latencyMillis }
        return EngineReport(
            engineName = engineName,
            queries = perQuery.size,
            meanNdcg10 = perQuery.map { it.ndcg10 }.average(),
            mrr = perQuery.map { it.mrr }.average(),
            meanRecall10 = perQuery.map { it.recall10 }.average(),
            meanRecall20 = perQuery.map { it.recall20 }.average(),
            p50LatencyMillis = percentile(latencies, 50.0),
            p95LatencyMillis = percentile(latencies, 95.0),
        )
    }

    /** Nearest-rank percentile; [latencies] need not be sorted. */
    fun percentile(latencies: List<Long>, percentile: Double): Long {
        if (latencies.isEmpty()) return 0
        val sorted = latencies.sorted()
        val rank = ceil(percentile / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    fun renderMarkdown(reports: List<EngineReport>, notes: String): String = buildString {
        appendLine("| engine | queries | nDCG@10 | MRR | Recall@10 | Recall@20 | p50 ms | p95 ms |")
        appendLine("|---|---:|---:|---:|---:|---:|---:|---:|")
        reports.forEach { r ->
            appendLine(
                "| ${r.engineName} | ${r.queries} | ${r.meanNdcg10.f()} | ${r.mrr.f()} | " +
                    "${r.meanRecall10.f()} | ${r.meanRecall20.f()} | ${r.p50LatencyMillis} | ${r.p95LatencyMillis} |"
            )
        }
        if (notes.isNotBlank()) {
            appendLine()
            appendLine(notes)
        }
    }

    private fun Double.f(): String = "%.4f".format(this)
}
