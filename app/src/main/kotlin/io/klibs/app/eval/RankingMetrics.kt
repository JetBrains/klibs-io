package io.klibs.app.eval

import kotlin.math.ln
import kotlin.math.pow

/**
 * Pure, deterministic ranking-quality metrics used to compare search engines on a fixed query set.
 *
 * Every function takes a [rankedIds] list (project ids in the order an engine returned them) and the
 * query's graded [EvaluationQuery.judgments]. Ids beyond position [k] are ignored where a cutoff applies.
 */
object RankingMetrics {

    /**
     * Normalized Discounted Cumulative Gain at [k], in `[0, 1]`.
     * Uses the exponential gain `2^grade - 1` with a `log2(rank + 1)` discount, and normalizes by the
     * ideal ranking of the same judgments. Returns `0.0` when there is nothing relevant to rank.
     */
    fun ndcgAtK(rankedIds: List<Int>, judgments: Map<Int, Int>, k: Int): Double {
        val idcg = idealDcg(judgments, k)
        if (idcg == 0.0) return 0.0
        return dcgAtK(rankedIds, judgments, k) / idcg
    }

    /** Discounted Cumulative Gain of [rankedIds] truncated at [k]. */
    fun dcgAtK(rankedIds: List<Int>, judgments: Map<Int, Int>, k: Int): Double =
        rankedIds.take(k).withIndex().sumOf { (index, id) ->
            gain(judgments[id] ?: 0) / discount(index)
        }

    /**
     * Reciprocal rank of the first relevant (grade > 0) result, or `0.0` if none appears.
     * Averaged across queries this yields MRR.
     */
    fun reciprocalRank(rankedIds: List<Int>, judgments: Map<Int, Int>): Double {
        rankedIds.forEachIndexed { index, id ->
            if ((judgments[id] ?: 0) > 0) return 1.0 / (index + 1)
        }
        return 0.0
    }

    /**
     * Recall at [k]: fraction of all relevant project ids that appear in the top [k] results.
     * Returns `0.0` when the query has no relevant ids.
     */
    fun recallAtK(rankedIds: List<Int>, relevantIds: Set<Int>, k: Int): Double {
        if (relevantIds.isEmpty()) return 0.0
        val hits = rankedIds.take(k).count { it in relevantIds }
        return hits.toDouble() / relevantIds.size
    }

    private fun idealDcg(judgments: Map<Int, Int>, k: Int): Double =
        judgments.values.filter { it > 0 }.sortedDescending().take(k)
            .withIndex().sumOf { (index, grade) -> gain(grade) / discount(index) }

    /** Exponential relevance gain; grade 0 contributes nothing. */
    private fun gain(grade: Int): Double = 2.0.pow(grade) - 1.0

    /** log2(rank + 1) discount for a zero-based [index] (rank = index + 1). */
    private fun discount(index: Int): Double = ln((index + 2).toDouble()) / ln(2.0)
}
