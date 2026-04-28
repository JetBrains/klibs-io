package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoHealthComponentsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ScmRepoHealthComponentsRepository : JpaRepository<ScmRepoHealthComponentsEntity, Int> {

    fun findByScmRepoId(scmRepoId: Int): ScmRepoHealthComponentsEntity?

    /**
     * Upserts the I- and P-side components for a repo, leaving the C/A and final score
     * columns untouched (so that M2's score job can fill them in afterwards).
     */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmRepoHealthComponentsEntity (
            scmRepoId,
            issueOpenedCount,
            issueClosedCount,
            medianIssueCloseDays,
            prOpenedCount,
            prMergedCount,
            medianPrMergeDays,
            iScore,
            pScore
        )
        VALUES (
            :scmRepoId,
            :issueOpenedCount,
            :issueClosedCount,
            :medianIssueCloseDays,
            :prOpenedCount,
            :prMergedCount,
            :medianPrMergeDays,
            :iScore,
            :pScore
        )
        ON CONFLICT(scmRepoId) DO UPDATE SET
            issueOpenedCount     = :issueOpenedCount,
            issueClosedCount     = :issueClosedCount,
            medianIssueCloseDays = :medianIssueCloseDays,
            prOpenedCount        = :prOpenedCount,
            prMergedCount        = :prMergedCount,
            medianPrMergeDays    = :medianPrMergeDays,
            iScore               = :iScore,
            pScore               = :pScore
    """)
    fun upsertIssuePrComponents(
        @Param("scmRepoId") scmRepoId: Int,
        @Param("issueOpenedCount") issueOpenedCount: Int,
        @Param("issueClosedCount") issueClosedCount: Int,
        @Param("medianIssueCloseDays") medianIssueCloseDays: Double?,
        @Param("prOpenedCount") prOpenedCount: Int,
        @Param("prMergedCount") prMergedCount: Int,
        @Param("medianPrMergeDays") medianPrMergeDays: Double?,
        @Param("iScore") iScore: Double?,
        @Param("pScore") pScore: Double?,
    )

    /**
     * Writes the C and A components and the final composed health score.
     * Leaves the I- and P-side columns untouched.
     */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmRepoHealthComponentsEntity (
            scmRepoId,
            scoreRecomputedTs,
            activeContributors,
            topContributorShare,
            cScore,
            aScore,
            healthScore
        )
        VALUES (
            :scmRepoId,
            :scoreRecomputedTs,
            :activeContributors,
            :topContributorShare,
            :cScore,
            :aScore,
            :healthScore
        )
        ON CONFLICT(scmRepoId) DO UPDATE SET
            scoreRecomputedTs   = :scoreRecomputedTs,
            activeContributors  = :activeContributors,
            topContributorShare = :topContributorShare,
            cScore              = :cScore,
            aScore              = :aScore,
            healthScore         = :healthScore
    """)
    fun upsertScoreComponents(
        @Param("scmRepoId") scmRepoId: Int,
        @Param("scoreRecomputedTs") scoreRecomputedTs: Instant,
        @Param("activeContributors") activeContributors: Int?,
        @Param("topContributorShare") topContributorShare: Double?,
        @Param("cScore") cScore: Double?,
        @Param("aScore") aScore: Double?,
        @Param("healthScore") healthScore: Int?,
    )

    /** Marks the time when the issue/PR sync last ran for this repo. Drives the issue/PR sync queue. */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmRepoHealthComponentsEntity (scmRepoId, lastIssueOrPrSyncTs)
        VALUES (:scmRepoId, :ts)
        ON CONFLICT(scmRepoId) DO UPDATE SET lastIssueOrPrSyncTs = :ts
    """)
    fun setLastIssueOrPrSyncTs(@Param("scmRepoId") scmRepoId: Int, @Param("ts") ts: Instant)

    /**
     * Schedules the next time the score job should pick this repo up. Used both for the regular
     * weekly cadence and for short retries when GitHub stats endpoints return 202 Accepted.
     */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmRepoHealthComponentsEntity (scmRepoId, nextHealthComputeTs)
        VALUES (:scmRepoId, :ts)
        ON CONFLICT(scmRepoId) DO UPDATE SET nextHealthComputeTs = :ts
    """)
    fun setNextHealthComputeTs(@Param("scmRepoId") scmRepoId: Int, @Param("ts") ts: Instant)
}
