package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.enums.IndexingRequestStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface IndexingRequestRepository : CrudRepository<IndexingRequestEntity, Long> {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
        UPDATE package_index_request
        SET status = :#{#newStatus.name()}
        WHERE id = :id and status != :#{#newStatus.name()}
        RETURNING *
    """, nativeQuery = true)
    fun updateStatus(id: Long, newStatus: IndexingRequestStatus): IndexingRequestEntity?

    @Query(value = """
        SELECT req.*
        FROM package_index_request req
        WHERE req.status = 'PENDING'
          AND req.next_attempt_ts IS NOT NULL
          AND req.next_attempt_ts < current_timestamp
        ORDER BY req.next_attempt_ts
        LIMIT 1
    """, nativeQuery = true)
    fun findFirstForIndexing(): IndexingRequestEntity?

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
        UPDATE package_index_request
        SET status = 'PENDING',
            failed_ts = current_timestamp,
            failed_attempts = failed_attempts + 1,
            last_error_message = :errorMessage,
            next_attempt_ts = :nextAttemptTs
        WHERE id = :id
    """, nativeQuery = true)
    fun markAsFailed(@Param("id") id: Long, @Param("nextAttemptTs") nextAttemptTs: Instant?, @Param("errorMessage") errorMessage: String?)

    fun countByStatus(status: IndexingRequestStatus): Long

    fun findByGroupIdAndArtifactIdAndVersion(
        groupId: String,
        artifactId: String,
        version: String
    ): IndexingRequestEntity?

    fun findAllByGroupIdAndArtifactId(
        groupId: String,
        artifactId: String
    ): List<IndexingRequestEntity>

    fun findAllByGroupIdAndArtifactIdAndVersion(
        groupId: String,
        artifactId: String,
        version: String
    ): List<IndexingRequestEntity>
}