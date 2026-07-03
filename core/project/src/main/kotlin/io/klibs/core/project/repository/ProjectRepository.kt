package io.klibs.core.project.repository

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.project.ProjectEntity
import java.time.Instant

interface ProjectRepository {

    fun insert(projectEntity: ProjectEntity): ProjectEntity

    fun updateLatestVersion(id: Int, latestVersion: String, latestVersionTs: Instant): ProjectEntity

    fun updateDescription(projectName: String, ownerLogin: String, description: String)

    fun updateDescription(id: Int, description: String)

    fun updateMinimizedReadme(id: Int, minimizedReadme: String?)

    fun updateReadmeEmbedding(id: Int, embedding: FloatArray)

    /**
     * Stores [embedding] into the given pgvector [columnName] of the `project` table.
     * [columnName] must be a known embedding column (validated to prevent SQL injection).
     */
    fun updateReadmeEmbedding(id: Int, columnName: String, embedding: FloatArray)

    fun updateOwnerId(projectId: Int, newOwnerId: Int)

    fun findById(id: Int): ProjectEntity?

    fun findByScmRepoId(scmRepoId: Int): ProjectEntity?

    fun findByNameAndScmRepoId(name: String, scmRepoId: Int): ProjectEntity?

    fun findByNameAndOwnerLogin(name: String, ownerLogin: String): ProjectEntity?

    fun findManyByProjectNameAndOwnerLogin(pairs: List<Pair<String, String>>): List<ProjectEntity>

    fun findWithoutDescription(): ProjectEntity?

    /**
     * Returns one project that has a README but no embedding yet, or null if none remain.
     * Used by the scheduled embedding backfill job.
     */
    fun findWithoutEmbedding(): ProjectEntity?

    /**
     * Returns one project that has a README but is missing at least one of the given embedding
     * [columnNames], or null if none remain. Each name must be a known embedding column.
     * Used by the scheduled backfill job to fill every embedding column.
     */
    fun findWithoutEmbedding(columnNames: List<String>): ProjectEntity?

    fun findWithoutTags(): ProjectEntity?

    fun findProjectsByPackages(groupId: String, artifactId: String?): Set<Int>

    /**
     * Returns platforms from project_index materialized view.
     * Returns null if project is not in project_index (i.e., has no packages).
     */
    fun findPlatformsById(projectId: Int): List<PackagePlatform>?

    fun findAllForSitemap(): List<SitemapProjectEntry>

    /**
     * Full recompute of `project.dependent_count` for every project in the DB. Used by the
     * scheduled refresh job.
     */
    fun recomputeAllDependentCounts()
}
