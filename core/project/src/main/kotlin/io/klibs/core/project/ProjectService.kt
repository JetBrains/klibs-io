package io.klibs.core.project

import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.project.entity.Marker
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.project.utils.normalizeTag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val packageService: PackageService,
    private val readmeService: ReadmeService,

    private val projectRepository: ProjectRepository,
    private val packageRepository: PackageRepository,
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val markerRepository: MarkerRepository,
    private val tagRepository: TagRepository,
    private val projectTagRepository: ProjectTagRepository,
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository,
) {
    @Transactional(readOnly = true)
    fun getProjectDetailsByName(ownerLogin: String, projectName: String): ProjectDetails? {
        val scmRepositoryEntity = scmRepositoryRepository.findByName(ownerLogin, projectName) ?: return null
        val projectEntity = requireNotNull(projectRepository.findByScmRepoId(scmRepositoryEntity.idNotNull)) {
            "Unable to find the corresponding project for an existing SCM repo: $scmRepositoryEntity"
        }

        // Check if project has any packages
        if (!packageRepository.existsByProjectId(projectEntity.idNotNull)) {
            return null
        }

        val projectPlatforms = packageRepository.findPlatformsOf(projectEntity.idNotNull)

        return projectEntity.toDetails(
            projectEntity = projectEntity,
            scmRepositoryEntity = scmRepositoryEntity,
            projectPlatforms = projectPlatforms,
            projectMarkers = markerRepository.findAllByProjectId(projectEntity.idNotNull),
            projectTags = tagRepository.getTagsByProjectId(projectEntity.idNotNull)
        )
    }

    @Transactional(readOnly = true)
    fun getProjectDetailsById(projectId: Int): ProjectDetails? {
        val projectEntity = projectRepository.findById(projectId) ?: return null
        val scmRepositoryEntity = requireNotNull(scmRepositoryRepository.findById(projectEntity.scmRepoId)) {
            "Unable to find the corresponding scm repository for an existing project: $projectEntity"
        }
        val projectPlatforms = packageRepository.findPlatformsOf(projectEntity.idNotNull)

        return projectEntity.toDetails(
            projectEntity = projectEntity,
            scmRepositoryEntity = scmRepositoryEntity,
            projectPlatforms = projectPlatforms,
            projectMarkers = markerRepository.findAllByProjectId(projectEntity.idNotNull),
            projectTags = tagRepository.getTagsByProjectId(projectEntity.idNotNull)
        )
    }

    @Transactional(readOnly = true)
    fun getLatestProjectPackages(ownerLogin: String, projectName: String): List<PackageOverview> {
        // TODO can be optimized into a single request
        val scmRepositoryEntity = scmRepositoryRepository.findByName(ownerLogin, projectName) ?: return emptyList()
        val projectEntity = requireNotNull(projectRepository.findByScmRepoId(scmRepositoryEntity.idNotNull)) {
            "Unable to find the corresponding project for an existing SCM repo: $scmRepositoryEntity"
        }
        return packageService.getLatestPackagesByProjectId(projectEntity.idNotNull)
    }

    @Transactional(readOnly = true)
    fun getProjectReadmeMd(ownerLogin: String, projectName: String): String? {
        val scmRepositoryId = scmRepositoryRepository.findIdByName(ownerLogin, projectName) ?: return null
        return readmeService.readReadmeMd(scmRepositoryId)
    }

    @Transactional(readOnly = true)
    fun getProjectReadmeHtml(ownerLogin: String, projectName: String): String? {
        val scmRepositoryId = scmRepositoryRepository.findIdByName(ownerLogin, projectName) ?: return null
        return readmeService.readReadmeHtml(scmRepositoryId)
    }

    @Transactional
    fun updateProjectDescription(projectName: String, ownerLogin: String, description: String) {
        projectRepository.updateDescription(projectName, ownerLogin, description)
    }

    @Transactional
    fun updateProjectTags(
        projectName: String,
        ownerLogin: String,
        tags: List<String>,
        tagsType: TagOrigin
    ): List<String> {
        val scmRepositoryEntity = scmRepositoryRepository.findByName(ownerLogin, projectName)
            ?: throw IllegalArgumentException("Project $ownerLogin/$projectName not found")
        val projectEntity = projectRepository.findByScmRepoId(scmRepositoryEntity.idNotNull)
            ?: throw IllegalArgumentException("Project $ownerLogin/$projectName not found")
        val projectId = projectEntity.idNotNull

        val normalizedTags = tags.asSequence()
            .map { normalizeTag(it) }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        if (normalizedTags.isEmpty()) {
            if (tagsType == TagOrigin.GITHUB) {
                projectTagRepository.deleteByProjectIdAndOrigin(projectId, tagsType)
                return emptyList()
            } else {
                throw IllegalArgumentException(
                    "No one tag passed the normalization filter." +
                            " Make sure that your tags are correctly normalized:" +
                            " they should be lowercase, and words should be separated by a dash. Example: `compose-ui`"
                )
            }
        }

        val invalidTags = mutableListOf<String>()
        val canonicalTagsToAdd = mutableListOf<String>()

        for (tag in normalizedTags) {
            val canonicalName = allowedProjectTagsRepository.findCanonicalNameByValue(tag)
            if (canonicalName == null) {
                invalidTags.add(tag)
            } else {
                canonicalTagsToAdd.add(canonicalName)
            }
        }

        if (invalidTags.isNotEmpty() && tagsType != TagOrigin.GITHUB) {
            throw IllegalArgumentException("Invalid tags were provided: ${invalidTags.joinToString(", ")}")
        }

        val tagsToSave = canonicalTagsToAdd.distinct()

        projectTagRepository.deleteByProjectIdAndOrigin(projectId, tagsType)

        val entities = tagsToSave.map { value ->
            TagEntity(
                projectId = projectId,
                value = value,
                origin = tagsType
            )
        }
        projectTagRepository.saveAll(entities)

        return tagsToSave
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProjectService::class.java)
    }
}

private fun ProjectEntity.toDetails(
    projectEntity: ProjectEntity,
    scmRepositoryEntity: ScmRepositoryEntity,
    projectPlatforms: List<PackagePlatform>,
    projectMarkers: List<Marker>,
    projectTags: List<String>,
): ProjectDetails {
    return ProjectDetails(
        id = this.idNotNull,
        ownerType = scmRepositoryEntity.ownerType,
        ownerLogin = scmRepositoryEntity.ownerLogin,
        name = scmRepositoryEntity.name,
        description = projectEntity.description ?: scmRepositoryEntity.description,
        platforms = projectPlatforms,
        latestReleaseVersion = projectEntity.latestVersion,
        latestReleasePublishedAt = projectEntity.latestVersionTs,
        linkHomepage = scmRepositoryEntity.homepage,
        hasGhPages = scmRepositoryEntity.hasGhPages,
        hasIssues = scmRepositoryEntity.hasIssues,
        hasWiki = scmRepositoryEntity.hasWiki,
        stars = scmRepositoryEntity.stars,
        createdAt = scmRepositoryEntity.createdTs,
        openIssues = scmRepositoryEntity.openIssues,
        lastActivityAt = scmRepositoryEntity.lastActivityTs,
        licenseName = scmRepositoryEntity.licenseName,
        updatedAt = scmRepositoryEntity.updatedAtTs,
        tags = projectTags,
        markers = projectMarkers.map { it.type }
    )
}
