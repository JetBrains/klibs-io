package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.integration.ai.ProjectReadmeEmbeddingGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProjectEmbeddingService(
    private val projectRepository: ProjectRepository,
    private val readmeService: ReadmeService,
    private val scmOwnerRepository: ScmOwnerRepository,
    private val projectReadmeEmbeddingGenerator: ProjectReadmeEmbeddingGenerator,
    @Qualifier("aiEmbeddingBackoffProvider")
    private val embeddingBackoffProvider: BackoffProvider,
) {

    fun addReadmeEmbedding(): Boolean {
        var selectedProjectId: Int? = null
        try {
            val project = projectRepository.findWithoutEmbedding() ?: return false
            if (embeddingBackoffProvider.isBackedOff(project.idNotNull)) {
                logger.debug("Selected projectId=${project.id} is in backoff for the embedding update; skipping this run")
                return true
            }
            selectedProjectId = project.idNotNull
            logger.trace("Generating a README embedding for projectId=${project.id}: ${project.name}")

            val embedding = projectReadmeEmbeddingGenerator.generateReadmeEmbedding(project.minimizedReadme!!)
            projectRepository.updateReadmeEmbedding(project.idNotNull, embedding)
            logger.debug("Updated README embedding for projectId=${project.id}: ${project.name}")

            embeddingBackoffProvider.onSuccess(project.idNotNull)
        } catch (e: Exception) {
            logger.error("Exception while updating README embedding", e)
            selectedProjectId?.let { embeddingBackoffProvider.onFailure(it) }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectEmbeddingService::class.java)
    }
}
