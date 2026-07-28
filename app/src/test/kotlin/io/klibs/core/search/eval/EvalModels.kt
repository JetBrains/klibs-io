package io.klibs.core.search.eval

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/** Models, mirroring the labeled query set in queries.json (KTL-4710). */

enum class EvalClass(
    /** represents our strategic focus; multiplicator for the normalized class mean */
    val weight: Double
) {
    /** Exact-name: project name -> that project at rank 1. Low traffic, but a hard expectation. */
    A(0.20),

    /** Category: keyword -> suitable libs. Real user intent, highest-traffic ranking pain. */
    B(0.40),

    /** Related-lib: "Hilt", "Room alternative" -> KMP equivalent. Semantic unlock. */
    C(0.30),

    /** Filtering and sorting: query + platform filter. Less weight cause simple. */
    D(0.15),

    /** Multi-term: compositional queries. Real user intent, prone to common-term dilution. */
    M(0.40),

    /** Query-mechanics: dotted/coordinate name, apostrophe, stop-words, fuzzy/typo. Must-have capability. */
    E(0.40),
}

/**
 * How a case is judged pass/fail.
 * A result counts as relevant if it is in the case's [EvalCase.expected] set *or* its [EvalCase.also] set.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(PassSpec.RankLe::class, name = "rank_le"),
    JsonSubTypes.Type(PassSpec.AnyInTop::class, name = "any_in_top"),
    JsonSubTypes.Type(PassSpec.PrecisionAt::class, name = "precision_at"),
    JsonSubTypes.Type(PassSpec.AllSupportPlatform::class, name = "all_support_platform"),
    JsonSubTypes.Type(PassSpec.NonEmpty::class, name = "non_empty"),
)
sealed class PassSpec {
    /** `rank_le`: a relevant lib must appear at rank <= [k]. */
    data class RankLe(val k: Int) : PassSpec()

    /** `any_in_top`: at least one relevant lib must be within the top [k]. */
    data class AnyInTop(val k: Int) : PassSpec()

    /** `precision_at`: at least [min] of the top [k] results must be relevant. */
    data class PrecisionAt(val k: Int, val min: Double) : PassSpec()

    /** `all_support_platform`: every one of the top [k] must support all requested platforms (default: all results). */
    data class AllSupportPlatform(val k: Int? = null) : PassSpec()

    /** `non_empty`: any result at all comes back — a coverage-gap probe, e.g. `pdf`. */
    data object NonEmpty : PassSpec()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EvalCase(
    val id: String,
    val cls: EvalClass,
    val query: String,
    /** Traffic factor, `1 + share` so `1..2`. Demand before grading is taken into account. */
    val weight: Double,
    /** A human checked this answer key against a real query, which doubles [effectiveWeight]. */
    val graded: Boolean = false,
    /** Primary answer, higher score */
    val expected: List<String> = emptyList(),
    /** Secondary answer, lower score */
    val also: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val pass: PassSpec,
) {
    val effectiveWeight: Double get() = if (graded) weight * GRADE_FACTOR else weight

    private companion object {
        const val GRADE_FACTOR = 2.0
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueriesFile(val cases: List<EvalCase>)

/** Regression floor: case ids proven to pass on the frozen snapshot. Regression asserts these stay green. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Floor(val ids: List<String> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
/** The previous eval run, kept in `build/` so a before/after diff always compares the same corpus. */
data class EvalRunRecord(val headline: Double = 0.0, val passing: List<String> = emptyList())

data class SearchResult(val key: String, val platforms: Set<String>)
