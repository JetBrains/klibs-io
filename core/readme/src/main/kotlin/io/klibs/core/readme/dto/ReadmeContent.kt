package io.klibs.core.readme.dto

data class ReadmeContent(
    val raw: String,
    val markdown: String,
    val html: String,
    val minimized: String,
)
