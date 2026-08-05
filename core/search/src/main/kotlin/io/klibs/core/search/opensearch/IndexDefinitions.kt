package io.klibs.core.search.opensearch

import java.security.MessageDigest

object IndexDefinitions {

    val PROJECT_SETTINGS = load("settings.json")
    val PROJECT_MAPPINGS = load("project-mappings.json")
    val PROJECT_DOC_SQL = load("project-doc.sql")

    val PACKAGE_SETTINGS = load("settings.json")
    val PACKAGE_MAPPINGS = load("package-mappings.json")
    val PACKAGE_DOC_SQL = load("package-doc.sql")

    fun shortSha256Hex(vararg parts: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("\u0000").toByteArray())
            .take(4)
            .joinToString("") { "%02x".format(it) }

    private fun load(name: String): String =
        IndexDefinitions::class.java.getResourceAsStream("/opensearch/$name")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("missing OpenSearch index definition resource: /opensearch/$name")
}
