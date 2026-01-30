package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.ai.ProjectTagsGenerator
import io.klibs.integration.maven.MavenArtifact
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ProjectIndexingService(
    private val readmeService: ReadmeService,
    private val projectDescriptionGenerator: ProjectDescriptionGenerator,

    private val projectRepository: ProjectRepository,
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val projectTagsGenerator: ProjectTagsGenerator,
    private val projectTagRepository: ProjectTagRepository,
    @Qualifier("aiDescriptionBackoffProvider")
    private val descriptionBackoffProvider: BackoffProvider,
    @Qualifier("aiTagsBackoffProvider")
    private val tagsBackoffProvider: BackoffProvider,
) {

    fun addAiDescription() {
        var selectedProjectId: Int? = null
        try {
            val project = projectRepository.findWithoutDescription() ?: return
            if (descriptionBackoffProvider.isBackedOff(project.idNotNull)) {
                logger.debug("Selected projectId=${project.id} is in backoff for description update; skipping this run")
                return
            }
            selectedProjectId = project.idNotNull

            val repo = scmRepositoryRepository.findById(project.scmRepoId) ?: error("Unable to find the repo: $project")
            logger.trace("Generating an AI description for projectId=${project.id}")

            val readmeMd = readmeService.readReadmeMd(project.scmRepoId)
                ?: error("Unable to generate the description due to missing or empty README.md for $project")

            // there can be some very long readmes... see https://github.com/robstoll/atrium
            val shortenedReadme = if (readmeMd.length >= 25_000) readmeMd.take(25_000) else readmeMd

            val description = projectDescriptionGenerator.generateProjectDescription(repo.name, shortenedReadme)
            projectRepository.updateDescription(project.idNotNull, description)
            logger.debug("Updated AI description for projectId=${project.id}")

            descriptionBackoffProvider.onSuccess(project.idNotNull)
        } catch (e: Exception) {
            logger.error("Exception while updating AI description", e)
            selectedProjectId?.let { descriptionBackoffProvider.onFailure(it) }
        }
    }

    fun addAiTags() {
        var selectedProjectId: Int? = null
        try {
            val project = projectRepository.findWithoutTags() ?: return
            if (tagsBackoffProvider.isBackedOff(project.idNotNull)) {
                logger.debug("Selected projectId=${project.id} is in backoff for the tags update; skipping this run")
                return
            }
            selectedProjectId = project.idNotNull
            val repo = scmRepositoryRepository.findById(project.scmRepoId) ?: error("Unable to find the repo: $project")
            logger.debug("Generating AI tags for projectId=${project.id}: ${repo.name}")

            val tags = projectTagsGenerator.generateTagsForProject(
                repo.name,
                project.description ?: "",
                repo.description ?: "",
                repo.minimizedReadme ?: ""
            ).map {
                TagEntity(
                    projectId = project.idNotNull,
                    value = it,
                    origin = TagOrigin.AI
                )
            }
            projectTagRepository.saveAll(tags)
            logger.debug("Updated AI tags for projectId=${project.id} ${repo.name}: ${tags.joinToString(",") { it.value }})")
            tagsBackoffProvider.onSuccess(project.idNotNull)
        } catch (e: Exception) {
            logger.error("Exception while updating AI tags", e)
            selectedProjectId?.let { tagsBackoffProvider.onFailure(it) }
        }
    }

    @Transactional
    fun save(
        mavenArtifact: MavenArtifact,
        scmRepositoryEntity: ScmRepositoryEntity,
    ): ProjectEntity {
        val entity = projectRepository.findByNameAndScmRepoId(scmRepositoryEntity.name, scmRepositoryEntity.idNotNull)
        return if (entity == null) {
            persist(
                mavenArtifact = mavenArtifact,
                scmRepositoryEntity = scmRepositoryEntity,
            )
        } else {
            update(
                entity = entity,
                mavenArtifact = mavenArtifact,
            )
        }
    }

    private fun update(
        entity: ProjectEntity,
        mavenArtifact: MavenArtifact,
    ): ProjectEntity {
        val isVersionMismatch = mavenArtifact.version != entity.latestVersion
        if (!isVersionMismatch) return entity

        val artifactReleasedAt = LocalDateTime.ofInstant(mavenArtifact.releasedAt, ZoneOffset.UTC)
        val entityReleasedAt = LocalDateTime.ofInstant(entity.latestVersionTs, ZoneOffset.UTC)

        val shouldUpdateToArtifactVersion = artifactReleasedAt.isAfter(entityReleasedAt)
        if (!shouldUpdateToArtifactVersion) return entity

        logger.trace("Updating latestVersion and latestVersionTs for {} with {}", entity.id, mavenArtifact)
        return projectRepository.updateLatestVersion(
            id = entity.idNotNull,
            latestVersion = mavenArtifact.version,
            latestVersionTs = requireNotNull(mavenArtifact.releasedAt)
        )
    }

    private fun persist(
        mavenArtifact: MavenArtifact,
        scmRepositoryEntity: ScmRepositoryEntity,
    ): ProjectEntity {
        logger.debug("Persisting a new project for {}", mavenArtifact)
        return projectRepository.insert(
            ProjectEntity(
                id = null, // to be set by the DB
                scmRepoId = scmRepositoryEntity.idNotNull,
                ownerId = scmRepositoryEntity.ownerId,
                name = scmRepositoryEntity.name,
                description = null, // to be set later
                minimizedReadme = scmRepositoryEntity.minimizedReadme,
                latestVersion = mavenArtifact.version,
                latestVersionTs = requireNotNull(mavenArtifact.releasedAt),
            )
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ProjectIndexingService::class.java)
    }
}
