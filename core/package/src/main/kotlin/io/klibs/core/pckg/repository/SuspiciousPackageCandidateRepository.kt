package io.klibs.core.pckg.repository

import io.klibs.core.pckg.dto.projection.ForkBanCandidateView
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM suspicious_package_candidate candidate
            WHERE candidate.status = 'PENDING'
              AND NOT (EXISTS (SELECT 1
                               FROM package own
                               WHERE own.project_id = candidate.project_id
                                 AND own.artifact_id = candidate.artifact_id
                                 AND own.group_id = candidate.group_id)
                  AND EXISTS (SELECT 1
                              FROM package other
                              WHERE other.project_id = candidate.project_id
                                AND other.artifact_id = candidate.artifact_id
                                AND other.group_id <> candidate.group_id))
        """,
        nativeQuery = true,
    )
    fun deleteStaleCandidates(): Int

    @Query(
        value = """
            SELECT candidate.project_id                     AS projectId,
                   candidate.artifact_id                    AS artifactId,
                   candidate.group_id                       AS groupId,
                   split_part(candidate.group_id, '.', 3)   AS suspectOwner,
                   repo_owner.login                         AS repoOwner,
                   repo.name                                AS repoName
            FROM suspicious_package_candidate candidate
                     JOIN project ON project.id = candidate.project_id
                     JOIN scm_repo repo ON repo.id = project.scm_repo_id
                     JOIN scm_owner repo_owner ON repo_owner.id = repo.owner_id
            WHERE candidate.status = 'PENDING'
              AND (candidate.group_id LIKE 'io.github.%' OR candidate.group_id LIKE 'com.github.%')
              AND lower(split_part(candidate.group_id, '.', 3)) <> lower(repo_owner.login)
              AND NOT EXISTS (SELECT 1
                              FROM banned_packages banned
                              WHERE banned.group_id = candidate.group_id
                                AND (banned.artifact_id = candidate.artifact_id OR banned.artifact_id IS NULL))
              AND NOT EXISTS (SELECT 1
                              FROM package_dependency dependency
                                       JOIN maven_coordinate dependency_coordinate
                                            ON dependency_coordinate.id = dependency.dep_maven_coordinate_id
                                       JOIN package dependant ON dependant.id = dependency.package_id
                              WHERE dependency_coordinate.group_id = candidate.group_id
                                AND dependency_coordinate.artifact_id = candidate.artifact_id
                                AND dependant.project_id IS DISTINCT FROM candidate.project_id)
            ORDER BY candidate.project_id, candidate.artifact_id, candidate.group_id
        """,
        nativeQuery = true,
    )
    fun findForkBanCandidates(): List<ForkBanCandidateView>

    @Modifying
    @Transactional
    @Query(
        """
            UPDATE SuspiciousPackageCandidateEntity candidate
            SET candidate.status = io.klibs.core.pckg.enums.CandidateStatus.RESOLVED,
                candidate.notes = :notes
            WHERE candidate.projectId = :projectId
              AND candidate.artifactId = :artifactId
              AND candidate.groupId = :groupId
              AND candidate.status = io.klibs.core.pckg.enums.CandidateStatus.PENDING
        """
    )
    fun markResolved(
        @Param("projectId") projectId: Int,
        @Param("artifactId") artifactId: String,
        @Param("groupId") groupId: String,
        @Param("notes") notes: String,
    ): Int
}
