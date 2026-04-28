package io.klibs.app.configuration.properties

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
     * Observed CVs for real OSS repos sit around 0.5–1.3; 1.0 puts the "average"
     * project near the middle of the score range.
     */
    val commitCvDenominator: Double = 1.0,

    /**
     * Median-days threshold for the issue-responsiveness median term.
     * I = 0.5·ratio_term + 0.5·max(0, 1 − median_days / threshold).
     */
    val issueMedianDaysThreshold: Double = 21.0,

    /**
     * Median-days threshold for the PR-management median term.
     * Aligns with τ_p = 5 days from Destefanis et al. 2025.
     */
    val prMedianDaysThreshold: Double = 5.0,

    /**
     * Denominator for the "active contributors" term of author diversity.
     * A = 0.6·min(1, active / target) + 0.4·(1 − top_share).
     */
    val activeContributorsTarget: Int = 5,
)
