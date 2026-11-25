package io.klibs.core.readme.service

import io.klibs.core.project.ANDROIDX_DEFAULT_DESCRIPTION
import io.klibs.core.project.ANDROIDX_GITHUB_OWNER
import io.klibs.core.project.ANDROIDX_GITHUB_UMBRELLA_PROJECT
import io.klibs.core.project.ANDROIDX_PREFIX
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.dto.AndroidxProjectDTO
import io.klibs.integration.github.model.ReadmeFetchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class AndroidxReadmesUpdatingService(
    private val projectRepository: ProjectRepository,
    private val androidxReadmeFetcher: AndroidxReadmeFetcher,
    private val androidxReadmeContentBuilder: AndroidxReadmeContentBuilder,
    private val readmeService: ReadmeService,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun syncReadmes() {
        projectRepository.findByOwnerLogin(ANDROIDX_GITHUB_OWNER)
            .filterNot { it.name == ANDROIDX_GITHUB_UMBRELLA_PROJECT }
            .map { AndroidxProjectDTO(id = it.idNotNull, name = it.name, description = it.description) }
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
        project: AndroidxProjectDTO,
        shouldUpdate: (markdown: String) -> Boolean = { true },
    ) {
        when (val result = androidxReadmeFetcher.getReadme(project.name)) {
            is ReadmeFetchResult.Content -> {
                if (shouldUpdate(result.markdown)) {
                    processReadmeContent(
                        content = result.markdown,
                        project = project
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
        project: AndroidxProjectDTO,
    ) {
        val readmeContent = androidxReadmeContentBuilder.buildFromMarkdown(
            readme = content,
            projectName = "$ANDROIDX_PREFIX${project.name}",
        )
        // The default description is a placeholder set in case of missing readme; once a real readme exists,
        // it should be deleted so an accurate description can be generated.
        val clearDefaultDescription = project.description == ANDROIDX_DEFAULT_DESCRIPTION

        transactionTemplate.executeWithoutResult {
            projectRepository.updateMinimizedReadme(project.id, readmeContent.minimized)
            if (clearDefaultDescription) {
                projectRepository.updateDescription(project.id, null)
            }
            readmeService.writeReadmeFiles(
                projectId = project.id,
                rawContent = readmeContent.raw,
                mdContent = readmeContent.markdown,
                htmlContent = readmeContent.html,
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AndroidxReadmesUpdatingService::class.java)
    }
}
