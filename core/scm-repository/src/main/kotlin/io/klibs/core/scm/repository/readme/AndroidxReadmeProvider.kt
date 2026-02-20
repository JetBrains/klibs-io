package io.klibs.core.scm.repository.readme

/**
 * Resolves README content for androidx projects from classpath resources.
 * Returns `null` when the project is not an androidx project or has no bundled readme.
 */
interface AndroidxReadmeProvider {
    fun resolve(projectId: Int, format: String): String?
}
