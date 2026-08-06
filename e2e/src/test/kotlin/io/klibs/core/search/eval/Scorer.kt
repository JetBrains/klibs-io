package io.klibs.core.search.eval

import kotlin.math.log2

/**
 * Search-eval scoring.
*/
object Scorer {

    const val NDCG_K = 10

    /**
    Two outputs per run: a pass/fail per case (what the tiers assert)
    * and a headline score (what tracks progress).
    */
    fun aggregate(outcomes: List<CaseOutcome>): RunReport {
        val byClass = outcomes.groupBy { it.case.cls }.mapValues { (cls, classOutcomes) ->
            val weightSum = classOutcomes.sumOf { it.case.effectiveWeight }
            ClassSummary(
                cls = cls,
                n = classOutcomes.size,
                passed = classOutcomes.count { it.pass },
                // weighted mean nDCG within a class; weight represents a query demand
                meanNdcg = if (weightSum != 0.0)
                    classOutcomes.sumOf { it.case.effectiveWeight * it.ndcg } / weightSum else 0.0,
                mrr = classOutcomes.sumOf { it.reciprocalRank } / classOutcomes.size,
            )
        }

        val classWeightSum = byClass.keys.sumOf { it.weight }

        // headline` = sum(class_weight * class_mean) / sum(class_weight)
        val headline = if (classWeightSum != 0.0)
            byClass.entries.sumOf { (cls, d) -> cls.weight * d.meanNdcg } / classWeightSum else 0.0
        return RunReport(
            outcomes = outcomes,
            headline = headline,
            byClass = byClass,
            rawPassed = outcomes.count { it.pass },
        )
    }

    fun scoreCase(case: EvalCase, results: List<SearchResult>): CaseOutcome {
        val relevance = relevanceGrades(case)
        val hits = results.mapIndexedNotNull { i, r -> if ((relevance[r.key] ?: 0) > 0) i + 1 else null }
        val best = hits.minOrNull()
        val (ok, detail) = when (val p = case.pass) {
            is PassSpec.NonEmpty -> results.isNotEmpty() to "result count = ${results.size}, needs >= 1"
            is PassSpec.AllSupportPlatform -> {
                val want = case.platforms.map { it.lowercase() }.toSet()
                val topK = p.k ?: results.size
                val top = results.take(topK)
                val violators = top.filterNot { it.platforms.containsAll(want) }.map { it.key }
                (top.isNotEmpty() && violators.isEmpty()) to
                        ("${top.size - violators.size}/${top.size} of top-$topK support ${want.sorted()}" +
                                if (violators.isNotEmpty()) "; violators: ${violators.take(3)}" else "")
            }
            is PassSpec.RankLe -> (best != null && best <= p.k) to
                    "rank of best relevant result = ${best ?: "none"}, needs <= ${p.k}"
            is PassSpec.AnyInTop -> hits.any { it <= p.k } to
                    "relevant hits in top-${p.k} = ${hits.filter { it <= p.k }}, needs >= 1"
            is PassSpec.PrecisionAt -> {
                val precision = hits.count { it <= p.k }.toDouble() / p.k
                (precision >= p.min) to "precision@${p.k} = ${"%.2f".format(precision)}, needs >= ${p.min}"
            }
        }
        val reciprocalRank = if (best != null) 1.0 / best else 0.0
        val ndcg = ndcgAt(results, relevance)
        val score = ndcg ?: if (ok) 1.0 else 0.0
        return CaseOutcome(case, ok, detail, best, reciprocalRank, score, results.take(5).map { it.key })
    }

    /**
     * nDCG@[k] in `[0,1]` — how well the top [k] is *ordered*, not just what it contains.
     *
     * Each result contributes its [relevanceGrades] grade (2 primary, 1 secondary, 0 irrelevant)
     * discounted by position: `grade / log2(rank + 1)`. `DCG` sums that over the actual results,
     * `IDCG` over the ideal ordering (every earnable grade, sorted high to low, packed from rank 1),
     * and nDCG is their ratio.
     *
     * 1.0 = perfect ordering, 0.0 = nothing relevant in the top [k], null when the case grades nothing.
     */
    internal fun ndcgAt(results: List<SearchResult>, relevance: Map<String, Int>, k: Int = NDCG_K): Double? {
        if (relevance.isEmpty()) return null
        val dcg = results.take(k).mapIndexed { i, r -> (relevance[r.key] ?: 0) / log2((i + 2).toDouble()) }.sum()
        val idcg = relevance.values.sortedDescending().take(k)
            .mapIndexed { i, grade -> grade / log2((i + 2).toDouble()) }.sum()
        return if (idcg != 0.0) dcg / idcg else 0.0
    }

    /**
     * Graded relevance: 2 for a primary ([EvalCase.expected]) lib, 1 for a secondary ([EvalCase.also]) one.
     */
    internal fun relevanceGrades(case: EvalCase): Map<String, Int> {
        val grades = LinkedHashMap<String, Int>()
        case.expected.forEach { grades[it.lowercase()] = 2 }
        case.also.forEach { grades.putIfAbsent(it.lowercase(), 1) }
        return grades
    }
}

data class CaseOutcome(
    val case: EvalCase,
    val pass: Boolean,
    val detail: String,
    val bestRank: Int?,
    val reciprocalRank: Double,
    val ndcg: Double,
    val top5: List<String>,
) {
    /** Assertion message spelling out the query, what was expected, and what actually ranked. */
    fun failureMessage(tier: String): String = buildString {
        append("$tier FAIL — ${case.id}   query=\"${case.query}\"")
        append("\n  ${"check".padEnd(9)}: $detail")
        append("\n  ${"primary".padEnd(9)}: ${case.expected.ifEmpty { listOf("<none>") }} (grade 2)")
        append("\n  ${"secondary".padEnd(9)}: ${case.also.ifEmpty { listOf("<none>") }} (grade 1)")
        append("\n  ${"returned".padEnd(9)}: ${top5.ifEmpty { listOf("<none>") }}")
    }
}

data class ClassSummary(val cls: EvalClass, val n: Int, val passed: Int, val meanNdcg: Double, val mrr: Double)

data class RunReport(
    val outcomes: List<CaseOutcome>,
    val headline: Double,
    val byClass: Map<EvalClass, ClassSummary>,
    val rawPassed: Int,
) {
    fun passingIds(): List<String> = outcomes.filter { it.pass }.map { it.case.id }
}
