package io.klibs.integration.github.model

import java.time.Instant

/**
 * A single pull-request snapshot from `/repos/{owner}/{repo}/pulls`, used to build the
 * OSS Health sliding window. [mergedAt] is populated only for PRs that were actually
 * merged; closed-but-unmerged PRs have [mergedAt]=null.
 */
data class GitHubPullRequest(
    val number: Int,
    val createdAt: Instant,
    val closedAt: Instant?,
    val mergedAt: Instant?,
    val updatedAt: Instant,
)
