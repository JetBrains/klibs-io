package io.klibs.integration.github.model

import java.time.Instant

/**
 * A single issue snapshot from `/repos/{owner}/{repo}/issues` (filtered to non-PRs),
 * used to build the OSS Health sliding window.
 */
data class GitHubIssue(
    val number: Int,
    val createdAt: Instant,
    val closedAt: Instant?,
    val updatedAt: Instant,
)
