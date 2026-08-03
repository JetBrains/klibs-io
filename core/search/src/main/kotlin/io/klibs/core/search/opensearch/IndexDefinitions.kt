package io.klibs.core.search.opensearch

object IndexDefinitions {

    val PROJECT_SETTINGS = load("settings.json")
    val PACKAGE_SETTINGS = load("settings.json")
    val PROJECT_MAPPINGS = load("project-mappings.json")
    val PACKAGE_MAPPINGS = load("package-mappings.json")

    private fun load(name: String): String =
        IndexDefinitions::class.java.getResourceAsStream("/opensearch/$name")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("missing OpenSearch index definition resource: /opensearch/$name")
}
