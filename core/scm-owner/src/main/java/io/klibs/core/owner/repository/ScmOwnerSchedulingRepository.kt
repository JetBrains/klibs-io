package io.klibs.core.owner.repository

import io.klibs.core.owner.entity.ScmOwnerSchedulingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface ScmOwnerSchedulingRepository : JpaRepository<ScmOwnerSchedulingEntity, Int> {

    /**
     * Computes `nextRetryAt` from the database clock, the same clock `ScmOwnerRepository.findForUpdate` compares with.
     */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ScmOwnerSchedulingEntity (scmOwnerId, nextRetryAt, retryAttempts, reason)
        VALUES (:scmOwnerId, current_timestamp + (:backoffDelaySeconds) second, :attempts, :reason)
        ON CONFLICT(scmOwnerId) DO UPDATE
            SET nextRetryAt = excluded.nextRetryAt,
                retryAttempts = excluded.retryAttempts,
                reason = excluded.reason
    """)
    fun scheduleNextRetry(
        @Param("scmOwnerId") scmOwnerId: Int,
        @Param("attempts") attempts: Int,
        @Param("backoffDelaySeconds") backoffDelaySeconds: Long,
        @Param("reason") reason: String,
    ): Int
}
