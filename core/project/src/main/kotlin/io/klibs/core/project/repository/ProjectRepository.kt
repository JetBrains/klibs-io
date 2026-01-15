package io.klibs.core.project.repository

import io.klibs.core.project.ProjectEntity
import java.time.Instant

interface ProjectRepository {

    fun insert(projectEntity: ProjectEntity): ProjectEntity

    fun updateLatestVersion(id: Int, latestVersion: String, latestVersionTs: Instant): ProjectEntity

    fun updateDescription(id: Int, description: String)

    fun findById(id: Int): ProjectEntity?

    fun findByScmRepoId(scmRepoId: Int): ProjectEntity?

    fun findWithoutDescription(): ProjectEntity?

    fun findWithoutTags(): ProjectEntity?

    fun findProjectsByPackages(groupId: String, artifactId: String?): Set<Int>
}
