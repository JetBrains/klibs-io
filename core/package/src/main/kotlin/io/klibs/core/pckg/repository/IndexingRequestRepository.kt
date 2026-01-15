package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.IndexingRequestEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface IndexingRequestRepository : CrudRepository<IndexingRequestEntity, Long> {

    @Query(value = """
        SELECT req.*
        FROM package_index_request req
        WHERE req.failed_attempts < 2
          AND NOT EXISTS (
              SELECT 1
              FROM banned_packages bp
              WHERE bp.group_id = req.group_id 
                AND (bp.artifact_id = req.artifact_id OR bp.artifact_id IS NULL)
          )
        ORDER BY req.released_ts DESC NULLS FIRST
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    fun findFirstForIndexing(): IndexingRequestEntity?

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE package_index_request
        SET failed_ts = current_timestamp,
            failed_attempts = failed_attempts + 1,
            last_error_message = :errorMessage
        WHERE id = :id
    """, nativeQuery = true)
    fun markAsFailed(@Param("id") id: Long, @Param("errorMessage") errorMessage: String?)

    @Modifying
    @Transactional
    @Query(value = """
        DELETE
        FROM package_index_request req
        WHERE req.reindex = false
          AND EXISTS (
              SELECT true
              FROM package p
              WHERE req.group_id = p.group_id
                AND req.artifact_id = p.artifact_id
                AND req.version = p.version
          )
    """, nativeQuery = true)
    fun removeRepeating(): Int
}