package io.klibs.core.scm.repository.health.entity

import java.time.Instant

enum class ScmRepoIssueEventType {
    ISSUE, PR;
}

data class ScmRepoIssueEventEntity(
    val scmRepoId: Int,

    /**
     * GitHub's per-repo issue/PR number (the integer in URLs like `/issues/42`).
     * Combined with [scmRepoId], this is the upsert dedup key — re-fetching the
     * same item updates in place rather than inserting a duplicate row.
     */
    val ghNumber: Int,

    /**
     * Distinguishes issues from pull requests. Aggregate queries filter by this
     * because the I sub-score is computed from issues and the P sub-score from PRs.
     */
    val type: ScmRepoIssueEventType,

    val createdAt: Instant,
    val closedAt: Instant?,
    val mergedAt: Instant?,
    val durationDays: Int?,
)
