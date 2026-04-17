package io.klibs.core.readme

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ClasspathAndroidxReadmeProvider : AndroidxReadmeProvider {

    override fun resolve(projectName: String, format: String): String? {
        val resourcePath = "androidx_readmes/$projectName.$format"
        return try {
            val content = javaClass.classLoader.getResourceAsStream(resourcePath)
                ?.bufferedReader()
                ?.readText()
            if (content == null) {
                logger.debug("No classpath readme resource for androidx project '{}': {}", projectName, resourcePath)
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