package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.enums.IndexingRequestStatus
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

interface IndexingRequestRepository : CrudRepository<IndexingRequestEntity, Long> {

    @Transactional
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
          AND req.failed_attempts < :maxAttempts
        ORDER BY req.released_ts DESC NULLS FIRST
        LIMIT 1
    """, nativeQuery = true)
    fun findFirstForIndexing(@Param("maxAttempts") maxAttempts: Int): IndexingRequestEntity?

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
        UPDATE package_index_request
        SET status = 'PENDING',
            failed_ts = current_timestamp,
            failed_attempts = failed_attempts + 1,
            last_error_message = :errorMessage
        WHERE id = :id
    """, nativeQuery = true)
    fun markAsFailed(@Param("id") id: Long, @Param("errorMessage") errorMessage: String?)

    fun findByGroupIdAndArtifactIdAndVersion(
        groupId: String,
        artifactId: String,
        version: String
    ): IndexingRequestEntity?
}