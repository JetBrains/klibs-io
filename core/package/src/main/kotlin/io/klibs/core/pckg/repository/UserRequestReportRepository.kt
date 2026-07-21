package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.UserRequestReportEntity
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface UserRequestReportRepository : CrudRepository<UserRequestReportEntity, Long> {

    @Query(
        """
            SELECT report
            FROM UserRequestReportEntity report
            WHERE report.failedAttempts < :#{@indexingRetryConfiguration.maxAttempts}
              AND (report.nextAttemptTs IS NULL OR report.nextAttemptTs <= :before)
            ORDER BY report.id
        """
    )
    fun findFirstForReportingBefore(@Param("before") before: Instant, limit: Limit): UserRequestReportEntity?

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(
        """
            UPDATE UserRequestReportEntity report
            SET report.nextAttemptTs = :nextAttemptTs
            WHERE report.id = :id
        """
    )
    fun deferUntil(@Param("id") id: Long, @Param("nextAttemptTs") nextAttemptTs: Instant)

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(
        """
            UPDATE UserRequestReportEntity report
            SET report.failedTs = current_timestamp,
                report.failedAttempts = report.failedAttempts + 1,
                report.lastErrorMessage = :errorMessage
            WHERE report.id = :id
        """
    )
    fun markAsFailed(@Param("id") id: Long, @Param("errorMessage") errorMessage: String?)
}