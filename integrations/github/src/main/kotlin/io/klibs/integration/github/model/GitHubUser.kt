package io.klibs.integration.github.model

data class GitHubUser(
    val id: Long,
    val login: String,
    val type: String,

    val name: String,
    val company: String?,
    val blog: String?,
    val location: String?,
    val email: String?,
    val bio: String?,
    val twitterUsername: String?,
    val followers: Int,
)
