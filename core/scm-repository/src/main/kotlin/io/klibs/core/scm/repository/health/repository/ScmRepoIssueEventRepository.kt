package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventEntity
import java.time.Instant

interface ScmRepoIssueEventRepository {

    fun upsert(entity: ScmRepoIssueEventEntity)

    fun upsertAll(entities: Collection<ScmRepoIssueEventEntity>)

    fun pruneOlderThan(scmRepoId: Int, olderThan: Instant): Int

    fun aggregate(scmRepoId: Int, windowStart: Instant): WindowAggregates

    /**
     * Aggregates over the 12-week sliding window for a single repo,
     * computed from [ScmRepoIssueEventEntity] rows. Nested because it is the
     * shape of the [aggregate] result and has no use outside this method.
     */
    data class WindowAggregates(
        val issueOpenedCount: Int,
        val issueClosedCount: Int,
        val medianIssueCloseDays: Double?,
        val prOpenedCount: Int,
        val prMergedCount: Int,
        val medianPrMergeDays: Double?,
    )
}
