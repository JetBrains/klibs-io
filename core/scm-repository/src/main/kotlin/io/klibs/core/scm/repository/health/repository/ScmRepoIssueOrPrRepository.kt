package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoIssueOrPrEntity
import io.klibs.core.scm.repository.health.enums.ScmRepoIssueOrPrType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ScmRepoIssueOrPrRepository : JpaRepository<ScmRepoIssueOrPrEntity, Long> {

    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmRepoIssueOrPrEntity (
            scmRepoId,
            ghNumber,
            type,
            createdAt,
            closedAt,
            mergedAt,
            durationDays
        )
        VALUES (
            :scmRepoId,
            :ghNumber,
            :type,
            :createdAt,
            :closedAt,
            :mergedAt,
            :durationDays
        )
        ON CONFLICT(scmRepoId, ghNumber) DO UPDATE SET
            type          = :type,
            createdAt     = :createdAt,
            closedAt      = :closedAt,
            mergedAt      = :mergedAt,
            durationDays  = :durationDays
    """)
    fun upsert(
        @Param("scmRepoId") scmRepoId: Int,
        @Param("ghNumber") ghNumber: Int,
        @Param("type") type: ScmRepoIssueOrPrType,
        @Param("createdAt") createdAt: Instant,
        @Param("closedAt") closedAt: Instant?,
        @Param("mergedAt") mergedAt: Instant?,
        @Param("durationDays") durationDays: Int?,
    )

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM ScmRepoIssueOrPrEntity
        WHERE scmRepoId = :scmRepoId
          AND createdAt < :olderThan
          AND (closedAt IS NULL OR closedAt < :olderThan)
    """)
    fun pruneOlderThan(@Param("scmRepoId") scmRepoId: Int, @Param("olderThan") olderThan: Instant): Int

    /**
     * Returns every issue/PR for the repo whose `createdAt`, `closedAt`, or `mergedAt` is on
     * or after [since] — i.e. anything that had at least one observable state change in
     * that period. Coarse prefilter for downstream aggregation; the precise per-metric
     * predicates ("opened since X", "closed since X", "merged since X") live in service code.
     */
    @Query("""
        SELECT e FROM ScmRepoIssueOrPrEntity e
        WHERE e.scmRepoId = :scmRepoId
          AND (
                e.createdAt >= :since
             OR (e.closedAt IS NOT NULL AND e.closedAt >= :since)
             OR (e.mergedAt IS NOT NULL AND e.mergedAt >= :since)
          )
    """)
    fun findActiveSince(
        @Param("scmRepoId") scmRepoId: Int,
        @Param("since") since: Instant,
    ): List<ScmRepoIssueOrPrEntity>
}
