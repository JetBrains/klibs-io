package io.klibs.core.project

import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.entity.Marker
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.scm.repository.ScmRepositoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompareService(
    private val projectRepository: ProjectRepository,
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val markerRepository: MarkerRepository,
    private val tagRepository: TagRepository,
    private val packageService: PackageService,
) {
    @Transactional(readOnly = true)
    fun compareProjects(refs: List<CompareProjectRequest.ProjectRef>): List<CompareProjectResponse?> {
        require(refs.size <= MAX_PROJECTS) { "Cannot compare more than $MAX_PROJECTS projects at once" }

        val projectEntities = refs.map { ref ->
            projectRepository.findByNameAndOwnerLogin(ref.projectName, ref.ownerLogin)
        }

        val projectIds = projectEntities.filterNotNull().map { it.idNotNull }

        val kotlinVersionsByProjectId = packageService.getKotlinVersionsByProjectIds(projectIds)

        return refs.mapIndexed { index, ref ->
            val projectEntity = projectEntities[index] ?: return@mapIndexed null

            val scmRepo = scmRepositoryRepository.findById(projectEntity.scmRepoId) ?: return@mapIndexed null
            val platforms = projectRepository.findPlatformsById(projectEntity.idNotNull) ?: return@mapIndexed null
            val markers = markerRepository.findAllByProjectId(projectEntity.idNotNull)
            val tags = tagRepository.getTagsByProjectId(projectEntity.idNotNull)

            val lastActivityAt = if (scmRepo.ownerLogin == AndroidxReadmeProvider.OWNER_NAME) {
                projectEntity.latestVersionTs
            } else {
                scmRepo.lastActivityTs
            }

            CompareProjectResponse(
                ownerLogin = ref.ownerLogin,
                projectName = ref.projectName,
                description = projectEntity.description ?: scmRepo.description,
                scmStars = scmRepo.stars,
                licenseName = scmRepo.licenseName,
                latestReleaseVersion = projectEntity.latestVersion,
                latestReleasePublishedAtMillis = projectEntity.latestVersionTs.toEpochMilli(),
                lastActivityAtMillis = lastActivityAt.toEpochMilli(),
                createdAtMillis = scmRepo.createdTs.toEpochMilli(),
                openIssues = scmRepo.openIssues,
                platforms = platforms.map { it.serializableName },
                tags = tags,
                markers = markers.map { it.type.name },
                kotlinVersion = kotlinVersionsByProjectId[projectEntity.idNotNull]
            )
        }
    }

    private companion object {
        private const val MAX_PROJECTS = 10
    }
}
