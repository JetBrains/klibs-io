package io.klibs.integration.github.model

import java.time.Instant

enum class GitHubIssueEventType { ISSUE, PR }

/**
 * A single issue or pull-request snapshot, used to build the OSS Health sliding window.
 * [closedAt] is populated when the item has been closed; [mergedAt] is only populated
 * for PRs that were actually merged.
 */
data class GitHubIssueEvent(
    val type: GitHubIssueEventType,
    val number: Int,
    val createdAt: Instant,
    val closedAt: Instant?,
    val mergedAt: Instant?,
    val updatedAt: Instant,
)
