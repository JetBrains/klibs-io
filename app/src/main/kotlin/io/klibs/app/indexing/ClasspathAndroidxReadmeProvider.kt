package io.klibs.app.indexing

import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.AndroidxReadmeProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ClasspathAndroidxReadmeProvider(
    private val projectRepository: ProjectRepository,
) : AndroidxReadmeProvider {

    override fun resolve(projectId: Int, format: String): String? {
        val project = projectRepository.findById(projectId) ?: return null

        val resourcePath = "androidx_readmes/${project.name}.$format"
        return try {
            val content = javaClass.classLoader.getResourceAsStream(resourcePath)
                ?.bufferedReader()
                ?.readText()
            if (content == null) {
                logger.debug("No classpath readme resource for androidx project '{}': {}", project.name, resourcePath)
            }
            content
        } catch (e: Exception) {
            logger.warn("Failed to read androidx readme resource: {}", resourcePath, e)
            null
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ClasspathAndroidxReadmeProvider::class.java)
    }
}
