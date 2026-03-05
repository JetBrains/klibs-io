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

    fun updateOwnerId(projectId: Int, newOwnerId: Int)

    fun findById(id: Int): ProjectEntity?

    fun findByScmRepoId(scmRepoId: Int): ProjectEntity?

    fun findByNameAndScmRepoId(name: String, scmRepoId: Int): ProjectEntity?

    fun findByNameAndOwnerLogin(name: String, ownerLogin: String): ProjectEntity?

    fun findWithoutDescription(): ProjectEntity?

    fun findWithoutTags(): ProjectEntity?

    fun findProjectsByPackages(groupId: String, artifactId: String?): Set<Int>

    /**
     * Returns platforms from project_index materialized view.
     * Returns null if project is not in project_index (i.e., has no packages).
     */
    fun findPlatformsById(projectId: Int): List<PackagePlatform>?
}
