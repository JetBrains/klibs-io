package io.klibs.core.scm.repository.health.entity

import java.time.Instant

data class ScmRepoHealthComponentsEntity(
    val scmRepoId: Int,
    val scoreRecomputedTs: Instant?,

    val issueOpenedCount: Int?,
    val issueClosedCount: Int?,
    val medianIssueCloseDays: Double?,

    val prOpenedCount: Int?,
    val prMergedCount: Int?,
    val medianPrMergeDays: Double?,

    val commitsCv: Double?,
    val activeContributors: Int?,
    val topContributorShare: Double?,

    val cScore: Double?,
    val iScore: Double?,
    val pScore: Double?,
    val aScore: Double?,

    val healthScore: Int?,

    /** Drives the event sync queue. */
    val lastEventSyncTs: Instant?,

    /** Drives the score compute queue (regular cadence + 202 retry delays). */
    val nextHealthComputeTs: Instant?,
)
