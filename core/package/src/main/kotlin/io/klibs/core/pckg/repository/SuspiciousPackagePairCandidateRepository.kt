package io.klibs.core.pckg.repository

import io.klibs.core.pckg.dto.projection.SuspiciousPackagePairView
import io.klibs.core.pckg.entity.SuspiciousPackagePairCandidateEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SuspiciousPackagePairCandidateRepository :
    CrudRepository<SuspiciousPackagePairCandidateEntity, Long> {

    /**
     * Within each project, finds every `(project_id, artifact_id, group_id)` entry whose
     * `artifact_id` is published under more than one `group_id`
     */
    @Query(value = """
        SELECT
            agg.project_id       AS projectId,
            agg.artifact_id      AS artifactId,
            agg.group_id         AS groupId,
            agg.version_count    AS versionCount,
            agg.first_release_ts AS firstReleaseTs,
            agg.last_release_ts  AS lastReleaseTs
        FROM (
            SELECT
                p.project_id,
                p.artifact_id,
                p.group_id,
                COUNT(*)::int     AS version_count,
                MIN(p.release_ts) AS first_release_ts,
                MAX(p.release_ts) AS last_release_ts,
                COUNT(*) OVER (PARTITION BY p.project_id, p.artifact_id) AS group_span
            FROM package p
            WHERE p.project_id IS NOT NULL
            GROUP BY p.project_id, p.artifact_id, p.group_id
        ) agg
        WHERE agg.group_span > 1
        """,
        nativeQuery = true)
    fun findConflictingPairs(): List<SuspiciousPackagePairView>
}
