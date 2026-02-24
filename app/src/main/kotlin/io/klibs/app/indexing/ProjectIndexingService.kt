package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.project.repository.ProjectTagRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.readme.GitHubIndexingReadmeContent
import io.klibs.core.readme.ReadmeContentBuilder
import io.klibs.core.readme.service.ReadmeServiceDispatcher
import io.klibs.integration.ai.ProjectDescriptionGenerator
import io.klibs.integration.ai.ProjectTagsGenerator
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.ReadmeFetchResult
import io.klibs.integration.maven.MavenArtifact
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ProjectIndexingService(
    private val readmeServiceDispatcher: ReadmeServiceDispatcher,
    private val projectDescriptionGenerator: ProjectDescriptionGenerator,

    private val projectRepository: ProjectRepository,
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val scmOwnerRepository: ScmOwnerRepository,

    private val projectTagsGenerator: ProjectTagsGenerator,
    private val projectTagRepository: ProjectTagRepository,
    private val gitHubIntegration: GitHubIntegration,
    private val readmeContentBuilder: ReadmeContentBuilder,
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
            logger.trace("Generating an AI description for projectId=${project.id}: ${project.name}")

            val ownerLogin = scmOwnerRepository.findById(project.ownerId)?.login
                ?: error("Unable to find owner for projectId=${project.id}")

            val readmeMd = readmeServiceDispatcher.readReadmeMd(
                ReadmeServiceDispatcher.ProjectInfo(
                    project.idNotNull,
                    project.scmRepoId,
                    project.name,
                    ownerLogin
                ),
            )
                ?: error("Unable to generate the description due to missing or empty README.md for $project")

            // there can be some very long readmes... see https://github.com/robstoll/atrium
            val shortenedReadme = if (readmeMd.length >= 25_000) readmeMd.take(25_000) else readmeMd

            val description = projectDescriptionGenerator.generateProjectDescription(
                project.name,
                shortenedReadme
            )
            projectRepository.updateDescription(project.idNotNull, description)
            logger.debug("Updated AI description for projectId=${project.id}: ${project.name}")

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
            logger.debug("Generating AI tags for projectId=${project.id}: ${project.name}")

            val readmeMd = readmeServiceDispatcher.readReadmeMd(
                ReadmeServiceDispatcher.ProjectInfo(
                    project.idNotNull,
                    project.scmRepoId,
                    project.name,
                    repo.ownerLogin
                ),
            )
                ?: error("Unable to generate the description due to missing or empty README.md for $project")

            // there can be some very long readmes... see https://github.com/robstoll/atrium
            val shortenedReadme = if (readmeMd.length >= 25_000) readmeMd.take(25_000) else readmeMd

            val tags = projectTagsGenerator.generateTagsForProject(
                project.name,
                project.description ?: "",
                repo.description ?: "",
                shortenedReadme
            ).map {
                TagEntity(
                    projectId = project.idNotNull,
                    value = it,
                    origin = TagOrigin.AI
                )
            }
            projectTagRepository.saveAll(tags)
            logger.debug("Updated AI tags for projectId=${project.id} ${project.name}: ${tags.joinToString(",") { it.value }}")
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
        val projectName = resolveProjectName(mavenArtifact, scmRepositoryEntity)
        val entity = projectRepository.findByNameAndScmRepoId(projectName, scmRepositoryEntity.idNotNull)
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
        if (scmRepositoryEntity.ownerLogin == AndroidxReadmeProvider.OWNER_NAME) {
            return persistAndroidxProject(mavenArtifact, scmRepositoryEntity)
        }

        val readmeContent = fetchReadmeContent(scmRepositoryEntity)

        logger.debug("Persisting a new project for {}", mavenArtifact)
        val persistedEntity = projectRepository.insert(
            ProjectEntity(
                id = null, // to be set by the DB
                scmRepoId = scmRepositoryEntity.idNotNull,
                ownerId = scmRepositoryEntity.ownerId,
                name = scmRepositoryEntity.name,
                description = null, // to be set later
                minimizedReadme = readmeContent?.minimized,
                latestVersion = mavenArtifact.version,
                latestVersionTs = requireNotNull(mavenArtifact.releasedAt),
            )
        )

        if (readmeContent != null) {
            scmRepositoryRepository.update(
                scmRepositoryEntity.copy(
                    hasReadme = readmeContent.markdown.isNotBlank()
                )
            )

            readmeServiceDispatcher.writeReadmeFiles(
                projectId = persistedEntity.idNotNull,
                mdContent = readmeContent.markdown,
                htmlContent = readmeContent.html
            )
        }

        return persistedEntity
    }

    private fun persistAndroidxProject(
        mavenArtifact: MavenArtifact,
        scmRepositoryEntity: ScmRepositoryEntity,
    ): ProjectEntity {
        val projectName = resolveProjectName(mavenArtifact, scmRepositoryEntity)

//      There used to be a code here to fetch and minimize readme to add minimizedReadme.
//      However, it will never trigger correctly.
//      A new androidx project will be persisted here if authors publish a new unknown androidx package.
//      For minimizedReadme to be updated, we would need to place README into resources folder beforehand.
//      I'd say it is very unlikely, and probably a new project will be persisted without minimizedReadme.

        logger.debug("Persisting a new androidx project {} by {}", projectName, mavenArtifact)

        return projectRepository.insert(
            ProjectEntity(
                id = null,
                scmRepoId = scmRepositoryEntity.idNotNull,
                ownerId = scmRepositoryEntity.ownerId,
                name = projectName,
                description = ANDROIDX_DEFAULT_DESCRIPTION,
                minimizedReadme = null,
                latestVersion = mavenArtifact.version,
                latestVersionTs = requireNotNull(mavenArtifact.releasedAt),
            )
        )
    }

    private fun fetchReadmeContent(scmRepositoryEntity: ScmRepositoryEntity): GitHubIndexingReadmeContent? {
        return when (val result = gitHubIntegration.getReadmeWithModifiedSinceCheck(
            scmRepositoryEntity.nativeId, Instant.EPOCH
        )) {
            is ReadmeFetchResult.Content -> readmeContentBuilder.buildFromMarkdown(
                readmeMd = result.markdown,
                nativeId = scmRepositoryEntity.nativeId,
                ownerLogin = scmRepositoryEntity.ownerLogin,
                repoName = scmRepositoryEntity.name,
                defaultBranch = scmRepositoryEntity.defaultBranch,
            )

            is ReadmeFetchResult.NotModified, is ReadmeFetchResult.Error, is ReadmeFetchResult.NotFound -> null
        }
    }

    private fun resolveProjectName(
        mavenArtifact: MavenArtifact,
        scmRepositoryEntity: ScmRepositoryEntity,
    ): String {
        if (scmRepositoryEntity.ownerLogin != AndroidxReadmeProvider.OWNER_NAME) {
            return scmRepositoryEntity.name
        }
        return mavenArtifact.groupId.split('.').getOrNull(1)
            ?: error("Unable to extract the project name from the groupId: ${mavenArtifact.groupId}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectIndexingService::class.java)
        private const val ANDROIDX_DEFAULT_DESCRIPTION =
            "This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility."
    }
}
