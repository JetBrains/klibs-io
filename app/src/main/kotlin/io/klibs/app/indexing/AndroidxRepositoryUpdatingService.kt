package io.klibs.app.indexing

import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.AndroidxReadmeContentBuilder
import io.klibs.core.readme.service.AndroidxReadmeFetcher
import io.klibs.core.readme.service.ReadmeService
import io.klibs.integration.github.model.ReadmeFetchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AndroidxRepositoryUpdatingService(
    private val projectRepository: ProjectRepository,
    private val androidxReadmeFetcher: AndroidxReadmeFetcher,
    private val androidxReadmeContentBuilder: AndroidxReadmeContentBuilder,
    private val readmeService: ReadmeService,
) {
    fun syncReadmes() {
        val projectsToUpdate = projectRepository.findByOwnerLogin("androidx")
        projectsToUpdate
            .filterNot { it.name == "androidx" } // Skip the androidx.androidx project
            .forEach { project ->
                syncReadme(project) {
                    val projectInfo = ReadmeService.ProjectInfo(id = project.id)
                    val oldContent = readmeService.readReadmeRaw(projectInfo)
                    return@syncReadme oldContent != it
                }
            }
    }

    fun syncReadme(
        project: ProjectEntity,
        isApplicable: (markdown: String) -> Boolean = { true },
    ) {
        when (val result = androidxReadmeFetcher.getReadme(project.name)) {
            is ReadmeFetchResult.Content -> {
                if (isApplicable(result.markdown)) {
                    processReadmeContent(
                        content = result.markdown,
                        projectEntity = project
                    )
                    logger.info("Readme for project androidx.${project.name} updated")
                } else {
                    logger.info("Readme for project androidx.${project.name} is up to date, skipping update")
                }
            }

            is ReadmeFetchResult.Error -> {
                logger.warn("Error fetching readme for project androidx.${project.name}: ${result.message}")
            }

            is ReadmeFetchResult.NotFound, ReadmeFetchResult.NotModified -> {
                logger.warn("Unexpected result of fetching readme for project androidx.${project.name}: ${result::class.qualifiedName}")
            }
        }
    }

    private fun processReadmeContent(
        content: String,
        projectEntity: ProjectEntity,
    ) {
        val readmeContent = androidxReadmeContentBuilder.buildFromMarkdown(
            readme = content,
            projectName = "androidx.${projectEntity.name}",
        )
        readmeService.writeReadmeFiles(
            projectId = projectEntity.idNotNull,
            rawContent = readmeContent.raw,
            mdContent = readmeContent.markdown,
            htmlContent = readmeContent.html,
        )
        projectEntity.minimizedReadme = readmeContent.minimized
        projectRepository.updateMinimizedReadme(projectEntity.idNotNull, readmeContent.minimized)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AndroidxRepositoryUpdatingService::class.java)
    }
}