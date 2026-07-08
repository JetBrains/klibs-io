package io.klibs.app.eval

/**
 * A single evaluation query together with its graded relevance judgments.
 *
 * [judgments] maps a project id to its relevance grade for this query, where a higher grade means
 * more relevant. Grade `0` (or a project id absent from the map) is treated as not relevant.
 * A 3-level grading (1 = marginal, 2 = relevant, 3 = perfect) is enough for klibs.
 */
data class EvaluationQuery(
    val id: String,
    val text: String,
    val judgments: Map<Int, Int>,
) {
    /** Project ids considered relevant (grade > 0), used for recall/MRR. */
    val relevantProjectIds: Set<Int> = judgments.filterValues { it > 0 }.keys
}

/** The full evaluation set loaded from a committed resource, plus its human-readable name. */
data class QuerySet(
    val name: String,
    val queries: List<EvaluationQuery>,
)
