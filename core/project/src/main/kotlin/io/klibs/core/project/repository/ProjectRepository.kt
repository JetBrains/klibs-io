package io.klibs.core.project.repository

import io.klibs.core.project.ProjectEntity
import java.time.Instant

interface ProjectRepository {

    fun insert(projectEntity: ProjectEntity): ProjectEntity

    fun updateLatestVersion(id: Int, latestVersion: String, latestVersionTs: Instant): ProjectEntity

    fun updateDescription(projectName: String, ownerLogin: String, description: String)

    fun updateDescription(id: Int, description: String)

    fun findById(id: Int): ProjectEntity?

    fun findByNameAndScmRepoId(name: String, scmRepoId: Int): ProjectEntity?

    fun findByNameAndOwnerLogin(name: String, ownerLogin: String): ProjectEntity?

    fun findWithoutDescription(): ProjectEntity?

    fun findWithoutTags(): ProjectEntity?

    fun findProjectsByPackages(groupId: String, artifactId: String?): Set<Int>
}
