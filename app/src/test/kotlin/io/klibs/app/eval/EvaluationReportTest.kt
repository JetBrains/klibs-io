package io.klibs.app.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EvaluationReportTest {

    @Test
    fun `aggregate averages metrics and reports latency percentiles`() {
        val perQuery = listOf(
            QueryMetrics(ndcg10 = 1.0, mrr = 1.0, recall10 = 1.0, recall20 = 1.0, latencyMillis = 10),
            QueryMetrics(ndcg10 = 0.0, mrr = 0.5, recall10 = 0.0, recall20 = 0.5, latencyMillis = 30),
        )

        val report = EvaluationReport.aggregate("fts", perQuery)

        assertEquals(2, report.queries)
        assertEquals(0.5, report.meanNdcg10, 1e-9)
        assertEquals(0.75, report.mrr, 1e-9)
        assertEquals(0.5, report.meanRecall10, 1e-9)
        assertEquals(0.75, report.meanRecall20, 1e-9)
    }

    @Test
    fun `aggregate of an empty list is all zeros`() {
        val report = EvaluationReport.aggregate("local", emptyList())
        assertEquals(0, report.queries)
        assertEquals(0.0, report.meanNdcg10, 1e-9)
    }

    @Test
    fun `percentile uses nearest-rank on unsorted input`() {
        val latencies = listOf(40L, 10L, 30L, 20L)
        assertEquals(20L, EvaluationReport.percentile(latencies, 50.0))
        assertEquals(40L, EvaluationReport.percentile(latencies, 95.0))
    }

    @Test
    fun `markdown report contains a row per engine`() {
        val reports = listOf(
            EvaluationReport.aggregate("fts", listOf(QueryMetrics(1.0, 1.0, 1.0, 1.0, 5))),
            EvaluationReport.aggregate("local", listOf(QueryMetrics(0.0, 0.0, 0.0, 0.0, 7))),
        )
        val markdown = EvaluationReport.renderMarkdown(reports, "- note")
        assertEquals(true, markdown.contains("| fts |"))
        assertEquals(true, markdown.contains("| local |"))
        assertEquals(true, markdown.contains("- note"))
    }
}
