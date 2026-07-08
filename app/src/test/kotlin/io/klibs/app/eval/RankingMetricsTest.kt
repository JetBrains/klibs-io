package io.klibs.app.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RankingMetricsTest {

    private val judgments = mapOf(1 to 3, 2 to 2, 3 to 1)

    @Test
    fun `ndcg is 1 when the ranking is ideal`() {
        val ndcg = RankingMetrics.ndcgAtK(listOf(1, 2, 3), judgments, k = 10)
        assertEquals(1.0, ndcg, 1e-9)
    }

    @Test
    fun `ndcg drops when highly relevant results are ranked lower`() {
        val ndcg = RankingMetrics.ndcgAtK(listOf(3, 2, 1), judgments, k = 10)
        // DCG=6.392789 / IDCG=9.392789
        assertEquals(0.680607, ndcg, 1e-6)
    }

    @Test
    fun `ndcg is 0 when there is nothing relevant to rank`() {
        assertEquals(0.0, RankingMetrics.ndcgAtK(listOf(1, 2), emptyMap(), k = 10), 1e-9)
    }

    @Test
    fun `reciprocal rank reflects the position of the first relevant result`() {
        assertEquals(0.5, RankingMetrics.reciprocalRank(listOf(9, 2, 7), judgments), 1e-9)
    }

    @Test
    fun `reciprocal rank is 0 when no relevant result appears`() {
        assertEquals(0.0, RankingMetrics.reciprocalRank(listOf(7, 8, 9), judgments), 1e-9)
    }

    @Test
    fun `recall counts relevant results within the cutoff`() {
        val relevant = setOf(2, 4, 6)
        assertEquals(2.0 / 3.0, RankingMetrics.recallAtK(listOf(1, 2, 3, 4), relevant, k = 10), 1e-9)
        assertEquals(1.0 / 3.0, RankingMetrics.recallAtK(listOf(1, 2, 3, 4), relevant, k = 2), 1e-9)
    }

    @Test
    fun `recall is 0 when the query has no relevant results`() {
        assertEquals(0.0, RankingMetrics.recallAtK(listOf(1, 2), emptySet(), k = 5), 1e-9)
    }
}
