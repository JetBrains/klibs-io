package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.readme.service.ReadmeService
import io.klibs.integration.ai.EmbedderRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProjectEmbeddingService(
    private val projectRepository: ProjectRepository,
    private val readmeService: ReadmeService,
    private val scmOwnerRepository: ScmOwnerRepository,
    private val embedderRegistry: EmbedderRegistry,
    @Qualifier("aiEmbeddingBackoffProvider")
    private val embeddingBackoffProvider: BackoffProvider,
) {

    fun addReadmeEmbedding(): Boolean {
        var selectedProjectId: Int? = null
        try {
            val embedders = embedderRegistry.all
            val project = projectRepository.findWithoutEmbedding(embedders.map { it.columnName }) ?: return false
            if (embeddingBackoffProvider.isBackedOff(project.idNotNull)) {
                logger.debug("Selected projectId=${project.id} is in backoff for the embedding update; skipping this run")
                return true
            }
            selectedProjectId = project.idNotNull
            logger.trace("Generating README embeddings for projectId=${project.id}: ${project.name}")

            val ownerLogin = scmOwnerRepository.findById(project.ownerId)?.login
                ?: error("Unable to find owner for projectId=${project.id}")

            val fullReadme = readmeService.readReadmeMd(
                ReadmeService.ProjectInfo(
                    project.idNotNull,
                    project.scmRepoId,
                    project.name,
                    ownerLogin,
                ),
            )?.takeIf { it.isNotBlank() }
                ?: project.minimizedReadme
                ?: error("Unable to generate embeddings due to missing README for projectId=${project.id}")

            // there can be some very long readmes... see https://github.com/robstoll/atrium
            val readme = if (fullReadme.length >= MAX_README_LENGTH) fullReadme.take(MAX_README_LENGTH) else fullReadme
            embedders.forEach { embedder ->
                val embedding = embedder.embed(readme)
                projectRepository.updateReadmeEmbedding(project.idNotNull, embedder.columnName, embedding)
            }
            logger.debug("Updated README embeddings for projectId=${project.id}: ${project.name}")

            embeddingBackoffProvider.onSuccess(project.idNotNull)
        } catch (e: Exception) {
            logger.error("Exception while updating README embedding", e)
            selectedProjectId?.let { embeddingBackoffProvider.onFailure(it) }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectEmbeddingService::class.java)

        // keep the embedding input within the OpenAI embedding token limit
        private const val MAX_README_LENGTH = 25_000
    }
}
