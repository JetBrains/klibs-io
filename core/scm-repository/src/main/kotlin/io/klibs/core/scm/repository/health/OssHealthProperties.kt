package io.klibs.core.scm.repository.health

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Tunable thresholds for the OSS Health Index sub-scores.
 *
 * Defaults are informed by an empirical run against the top-10 klibs.io repos
 * (see docs/commit message for the dataset). These are not absolute — the paper
 * this formula is loosely based on (Destefanis et al., 2025) describes similar
 * constants as "educated estimations... initial proposals". Re-tune in yaml as
 * more data accumulates.
 */
@ConfigurationProperties(prefix = "klibs.oss-health")
data class OssHealthProperties(
    /**
     * Denominator in C = max(0, 1 − CV / denominator).
     * Calibrated against the live catalog CV distribution (≈520 repos): median CV ≈ 1.8,
     * with a hard pileup at √11 ≈ 3.32 for repos active in only one of the last 12 weeks.
     * 3.0 scores consistently-committing repos well (CV ≈ 0.5–1.1 → C ≈ 0.6–0.8) while
     * still flooring the genuinely sporadic tail (CV ≥ 3) at 0.
     */
    val commitCvDenominator: Double = 3.0,

    /**
     * Median-days threshold for the issue-responsiveness median term.
     * I = 0.5·ratio_term + 0.5·max(0, 1 − median_days / threshold).
     */
    val issueMedianDaysThreshold: Double = 21.0,

    /**
     * Median-days threshold for the PR-management median term.
     * The paper's τ_p = 5 days proved too strict for thorough-review libraries (catalog
     * PR-merge medians: p75 ≈ 2d, p90 ≈ 15d); 10 days credits careful-but-timely review
     * without rewarding the genuinely-slow tail.
     */
    val prMedianDaysThreshold: Double = 10.0,

    /**
     * Denominator for the "active contributors" term of author diversity.
     * A = 0.6·min(1, active / target) + 0.4·(1 − top_share).
     */
    val activeContributorsTarget: Int = 5,
)
