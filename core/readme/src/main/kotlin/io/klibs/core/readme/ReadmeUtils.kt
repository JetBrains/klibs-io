package io.klibs.core.readme

const val ANDROIDX_GITHUB_OWNER = "androidx"

data class ReadmeContent(
    val raw: String,
    val markdown: String,
    val html: String,
    val minimized: String,
)
