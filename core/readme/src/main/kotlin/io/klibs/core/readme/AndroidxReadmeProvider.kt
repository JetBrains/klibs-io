package io.klibs.core.readme

/**
 * Resolves README content for androidx projects from classpath resources.
 * Returns `null` when the project has no bundled readme.
 */
interface AndroidxReadmeProvider {
    fun resolve(projectName: String, format: String): String?

    companion object {
        const val OWNER_NAME = "androidx"
    }
}
