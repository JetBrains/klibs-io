package io.klibs.core.readme.service

import io.klibs.core.project.ANDROIDX_DEFAULT_DESCRIPTION
import io.klibs.core.project.ANDROIDX_GITHUB_OWNER
import io.klibs.core.project.ANDROIDX_GITHUB_UMBRELLA_PROJECT
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class AndroidxReadmesUpdatingService(
    private val projectRepository: ProjectRepository,
    private val androidxReadmeFetcher: AndroidxReadmeFetcher,
    private val androidxReadmeContentBuilder: AndroidxReadmeContentBuilder,
    private val readmeService: ReadmeService,
    private val transactionTemplate: TransactionTemplate,
) {
    fun syncReadmes() {
        projectRepository.findByOwnerLogin(ANDROIDX_GITHUB_OWNER)
            .filterNot { it.name == ANDROIDX_GITHUB_UMBRELLA_PROJECT }
            .forEach { project ->
                runCatching {
                    syncReadme(project) { markdown ->
                        val projectInfo = ReadmeService.ProjectInfo(id = project.id)
                        val oldContent = readmeService.readReadmeRaw(projectInfo)
                        oldContent != markdown
                    }
                }.onFailure { logger.error("Failed to sync readme for $ANDROIDX_PREFIX${project.name}", it) }
            }
    }

    fun syncReadme(
        project: ProjectEntity,
        shouldUpdate: (markdown: String) -> Boolean = { true },
    ) {
        when (val result = androidxReadmeFetcher.getReadme(project.name)) {
            is ReadmeFetchResult.Content -> {
                if (shouldUpdate(result.markdown)) {
                    processReadmeContent(
                        content = result.markdown,
                        projectEntity = project
                    )
                    logger.info("Readme for project $ANDROIDX_PREFIX${project.name} updated")
                } else {
                    logger.info("Readme for project $ANDROIDX_PREFIX${project.name} is up to date, skipping update")
                }
            }

            is ReadmeFetchResult.Error -> {
                logger.warn("Error fetching readme for project $ANDROIDX_PREFIX${project.name}: (${result.status}) ${result.message}")
            }

            is ReadmeFetchResult.NotFound, ReadmeFetchResult.NotModified -> {
                logger.warn("Unexpected result of fetching readme for project $ANDROIDX_PREFIX${project.name}: ${result::class.qualifiedName}")
            }
        }
    }

    private fun processReadmeContent(
        content: String,
        projectEntity: ProjectEntity,
    ) {
        val readmeContent = androidxReadmeContentBuilder.buildFromMarkdown(
            readme = content,
            projectName = "$ANDROIDX_PREFIX${projectEntity.name}",
        )
        // The default description is a placeholder set in case of missing readme; once a real readme exists,
        // it should be deleted so an accurate description can be generated.
        val clearDefaultDescription = projectEntity.description == ANDROIDX_DEFAULT_DESCRIPTION

        transactionTemplate.executeWithoutResult {
            projectRepository.updateMinimizedReadme(projectEntity.idNotNull, readmeContent.minimized)
            if (clearDefaultDescription) {
                projectRepository.updateDescription(projectEntity.idNotNull, null)
            }
            readmeService.writeReadmeFiles(
                projectId = projectEntity.idNotNull,
                rawContent = readmeContent.raw,
                mdContent = readmeContent.markdown,
                htmlContent = readmeContent.html,
            )
        }

        projectEntity.minimizedReadme = readmeContent.minimized
        if (clearDefaultDescription) {
            projectEntity.description = null
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AndroidxReadmesUpdatingService::class.java)
        private const val ANDROIDX_PREFIX = "androidx."
    }
}
