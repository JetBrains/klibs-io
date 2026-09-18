package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface SuspiciousPackageCandidateRepository :
    CrudRepository<SuspiciousPackageCandidateEntity, SuspiciousPackageCandidateKey> {

    @Modifying
    @Transactional
    @Query(
        value = """
            INSERT INTO suspicious_package_candidate (project_id, artifact_id, group_id, status, detected_at)
            SELECT project_id, artifact_id, group_id, 'PENDING', now()
            FROM (SELECT project_id,
                         artifact_id,
                         group_id,
                         count(*) OVER (PARTITION BY project_id, artifact_id) AS group_id_count
                  FROM (SELECT DISTINCT project_id, artifact_id, group_id
                        FROM package
                        WHERE project_id IS NOT NULL) distinct_entries) counted
            WHERE group_id_count > 1
            ON CONFLICT (project_id, artifact_id, group_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertMissingCandidates(): Int
}
