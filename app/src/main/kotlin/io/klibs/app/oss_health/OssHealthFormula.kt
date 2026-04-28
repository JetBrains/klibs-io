package io.klibs.app.oss_health

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

/**
 * Pure computation helpers for the OSS Health Index.
 *
 *     OpenSourceHealth = 100 * (0.30*C + 0.25*I + 0.25*P + 0.20*A)
 *
 * All component scores are clamped to `[0, 1]`. Any component computed over an empty
 * sample is returned as `null`, which propagates through the final composition as `null`
 * (meaning "insufficient data").
 *
 * Inspired by Destefanis, Bartolucci, Graziotin, Neykova, Ortu — "Introducing Repository
 * Stability" (arXiv:2504.00542v1, 2025). Intentionally deviates from the paper in four ways:
 *  1. Monotonic sub-scores (more-is-better) rather than the paper's triangular φ_k(x)
 *     peaked at a target, because klibs.io consumers want "this library is well-maintained"
 *     not "this library is at equilibrium" — a very active repo shouldn't be penalized.
 *  2. Windowed (last 12 weeks) rather than cumulative ratios — fairer to mature repos.
 *  3. Additive `0.5·ratio + 0.5·median` for I and P instead of the paper's multiplicative
 *     `ratio × 1/(1+t)` with ambiguous time units.
 *  4. Contributor-based A (active count + top-share) instead of the paper's comment/
 *     active-user ratios — cheaper to compute and maps to the "bus factor" intuition.
 *
 * Threshold constants (CV denominator, median-day limits, target contributor count) are
 * passed in rather than hardcoded; see `OssHealthProperties` for the wired-up defaults.
 */
object OssHealthFormula {

    const val WEIGHT_C = 0.30
    const val WEIGHT_I = 0.25
    const val WEIGHT_P = 0.25
    const val WEIGHT_A = 0.20

    /**
     * Commit Consistency from 12 most recent weekly bucket counts.
     *
     *     C = max(0, 1 - CV(weekly_commits) / cvDenominator)
     *     CV = stddev / mean
     *
     * Returns `null` for an empty sample or a period with zero commits (mean = 0 ⇒ CV undefined).
     */
    fun commitConsistency(weeklyCommits: List<Int>, cvDenominator: Double): Double? {
        if (weeklyCommits.isEmpty()) return null
        val n = weeklyCommits.size
        val mean = weeklyCommits.sumOf { it.toDouble() } / n
        if (mean <= 0.0) return null
        val variance = weeklyCommits.sumOf { (it - mean) * (it - mean) } / n
        val stddev = sqrt(variance)
        val cv = stddev / mean
        return (1.0 - cv / cvDenominator).coerceIn(0.0, 1.0)
    }

    /**
     * Issue Responsiveness.
     *
     *     IssueCloseRatio = closed / opened
     *     I = 0.5 * min(1, IssueCloseRatio / 0.4)
     *       + 0.5 * max(0, 1 - MedianIssueCloseDays / medianDaysThreshold)
     *
     * Returns `null` when [opened] == 0 (ratio undefined) AND there's no median either.
     * When opened > 0 but no issues have been closed, ratio = 0 and median is treated as "worst" (0 score).
     */
    fun issueResponsiveness(
        opened: Int,
        closed: Int,
        medianCloseDays: Double?,
        medianDaysThreshold: Double,
    ): Double? {
        if (opened == 0 && medianCloseDays == null) return null
        val ratio = if (opened > 0) closed.toDouble() / opened else 0.0
        val ratioScore = (ratio / 0.4).coerceIn(0.0, 1.0)
        val medianScore = medianCloseDays?.let { (1.0 - it / medianDaysThreshold).coerceIn(0.0, 1.0) } ?: 0.0
        return (0.5 * ratioScore + 0.5 * medianScore).coerceIn(0.0, 1.0)
    }

    /**
     * PR Management.
     *
     *     PRMergeRatio = merged / opened
     *     P = 0.5 * min(1, PRMergeRatio / 0.5)
     *       + 0.5 * max(0, 1 - MedianPRMergeDays / medianDaysThreshold)
     */
    fun prManagement(
        opened: Int,
        merged: Int,
        medianMergeDays: Double?,
        medianDaysThreshold: Double,
    ): Double? {
        if (opened == 0 && medianMergeDays == null) return null
        val ratio = if (opened > 0) merged.toDouble() / opened else 0.0
        val ratioScore = (ratio / 0.5).coerceIn(0.0, 1.0)
        val medianScore = medianMergeDays?.let { (1.0 - it / medianDaysThreshold).coerceIn(0.0, 1.0) } ?: 0.0
        return (0.5 * ratioScore + 0.5 * medianScore).coerceIn(0.0, 1.0)
    }

    /**
     * Author Diversity.
     *
     *     A = 0.6 * min(1, ActiveContributors / activeContributorsTarget)
     *       + 0.4 * (1 - TopContributorCommitShare)
     *
     * [activeContributors] = distinct contributors with at least 1 commit in the last 12 weeks.
     * [topContributorShare] = top committer's commits / total commits in the same window. Range [0, 1].
     */
    fun authorDiversity(
        activeContributors: Int,
        topContributorShare: Double?,
        activeContributorsTarget: Int,
    ): Double? {
        if (activeContributors <= 0) return null
        val share = topContributorShare?.coerceIn(0.0, 1.0) ?: return null
        val contributorsScore = (activeContributors.toDouble() / activeContributorsTarget).coerceIn(0.0, 1.0)
        val diversityScore = (1.0 - share).coerceIn(0.0, 1.0)
        return (0.6 * contributorsScore + 0.4 * diversityScore).coerceIn(0.0, 1.0)
    }

    /**
     * Composes the final score. Returns `null` if any non-null component is missing in a way
     * that leaves less than half of the weight represented — specifically, returns `null` if
     * any of C/I/P/A is missing. This is intentional: we'd rather surface "insufficient data"
     * than a misleading score.
     */
    fun composeScore(c: Double?, i: Double?, p: Double?, a: Double?): Int? {
        if (c == null || i == null || p == null || a == null) return null
        val raw = 100.0 * (WEIGHT_C * c + WEIGHT_I * i + WEIGHT_P * p + WEIGHT_A * a)
        return raw.coerceIn(0.0, 100.0).toInt()
    }

    fun Double.toBigDecimalOrNull4(): BigDecimal = BigDecimal(this).setScale(4, RoundingMode.HALF_UP)
    fun Double.toBigDecimal2(): BigDecimal = BigDecimal(this).setScale(2, RoundingMode.HALF_UP)
}
