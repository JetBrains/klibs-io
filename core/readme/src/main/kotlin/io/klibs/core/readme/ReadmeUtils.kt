package io.klibs.core.readme

const val ANDROIDX_GITHUB_OWNER = "androidx"
const val ANDROIDX_DEFAULT_DESCRIPTION =
    "This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility."

data class ReadmeContent(
    val raw: String,
    val markdown: String,
    val html: String,
    val minimized: String,
)
