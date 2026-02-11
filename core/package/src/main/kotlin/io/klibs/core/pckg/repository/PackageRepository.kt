package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.dto.projection.PackageVersionsView
import io.klibs.core.pckg.model.PackagePlatform
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface PackageRepository: CrudRepository<PackageEntity, Long> {

    @Query(value = """
        SELECT
            combined.group_id     AS groupId,
            combined.artifact_id  AS artifactId,
            ARRAY_AGG(combined.version ORDER BY combined.version) AS versions
        FROM (
                 SELECT group_id, artifact_id, version, scraper_type FROM package
                 UNION ALL
                 SELECT group_id, artifact_id, version, scraper_type FROM package_index_request
             ) AS combined
        WHERE scraper_type != 'GOOGLE_MAVEN'
        GROUP BY combined.group_id, combined.artifact_id;
        """,
        nativeQuery = true)
    fun findAllKnownMavenCentralPackages(): List<PackageVersionsView>

    @Query(value = """
            WITH LatestVersions AS (
                SELECT DISTINCT ON (p.group_id, p.artifact_id)
                    p.group_id,
                    p.artifact_id,
                    p.description,
                    p.generated_description
                FROM package p
                WHERE p.description IS NOT NULL
                ORDER BY p.group_id, p.artifact_id, p.release_ts DESC
            )
            SELECT description
            FROM LatestVersions
            WHERE generated_description = false
            GROUP BY description
            HAVING COUNT(*) > 1
            LIMIT :limit
        """,
        nativeQuery = true)
    fun findDuplicateDescriptions(limit: Int = 1): List<String>

    fun findAllByDescription(description: String): List<PackageEntity>

    fun existsByProjectId(projectId: Int): Boolean

    fun findByGroupIdAndArtifactIdAndVersion(groupId: String, artifactId: String, version: String): PackageEntity?

    fun findByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId: String, artifactId: String): List<PackageEntity>

    fun findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId: String, artifactId: String): PackageEntity?

    @Query(value = """
            WITH latest_package_ids AS (SELECT DISTINCT ON (group_id, artifact_id) id
            FROM package
            WHERE project_id = :projectId
            ORDER BY group_id, artifact_id, release_ts DESC)
            SELECT DISTINCT platform
            FROM package_target
            WHERE package_id IN (SELECT id FROM latest_package_ids)
        """,
        nativeQuery = true)
    fun findPlatformsOf(projectId: Int): List<PackagePlatform>

    @Query(value = """
            WITH latest_package_ids AS (SELECT DISTINCT ON (group_id, artifact_id) id
                                        FROM package
                                        WHERE project_id = :projectId
                                        ORDER BY group_id, artifact_id, release_ts DESC)
            SELECT *
            FROM package
            WHERE id IN (SELECT id FROM latest_package_ids)
            ORDER BY group_id, artifact_id;
        """,
        nativeQuery = true)
    fun findLatestByProjectId(projectId: Int): List<PackageEntity>

    @Query(value = """
            WITH latest_package_ids AS (SELECT DISTINCT ON (group_id, artifact_id) id
                                        FROM package
                                        WHERE group_id = :groupId
                                        ORDER BY group_id, artifact_id, release_ts DESC)
            SELECT *
            FROM package
            WHERE id IN (SELECT id FROM latest_package_ids)
            ORDER BY group_id, artifact_id;
         """,
        nativeQuery = true)
    fun findLatestByGroupId(groupId: String): List<PackageEntity>
}
