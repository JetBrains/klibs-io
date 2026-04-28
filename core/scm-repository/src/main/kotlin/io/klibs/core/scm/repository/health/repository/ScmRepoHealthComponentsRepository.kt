package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoHealthComponentsEntity
import java.time.Instant

interface ScmRepoHealthComponentsRepository {

    fun findById(scmRepoId: Int): ScmRepoHealthComponentsEntity?

    /**
     * Upserts the I- and P-side components for a repo, leaving the C/A and final score
     * columns untouched (so that M2's score job can fill them in afterwards).
     */
    fun upsertIssuePrComponents(
        scmRepoId: Int,
        issueOpenedCount: Int,
        issueClosedCount: Int,
        medianIssueCloseDays: Double?,
        prOpenedCount: Int,
        prMergedCount: Int,
        medianPrMergeDays: Double?,
        iScore: Double?,
        pScore: Double?,
    )

    /**
     * Writes the C and A components and the final composed health score.
     * Leaves the I- and P-side columns untouched.
     */
    fun upsertScoreComponents(
        scmRepoId: Int,
        scoreRecomputedTs: Instant,
        commitsCv: Double?,
        activeContributors: Int?,
        topContributorShare: Double?,
        cScore: Double?,
        aScore: Double?,
        healthScore: Int?,
    )

    /** Marks the time when the event sync job last ran for this repo. Drives the event sync queue. */
    fun setLastEventSyncTs(scmRepoId: Int, ts: Instant)

    /**
     * Schedules the next time the score job should pick this repo up. Used both for the regular
     * weekly cadence and for short retries when GitHub stats endpoints return 202 Accepted.
     */
    fun setNextHealthComputeTs(scmRepoId: Int, ts: Instant)
}
