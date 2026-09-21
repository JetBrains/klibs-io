package io.klibs.integration.github.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

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
) {
    companion object {
        /** Single place where GitHub's blank-vs-null inconsistency is normalized away. */
        fun of(
            id: Long,
            login: String,
            type: String,
            name: String?,
            company: String?,
            blog: String?,
            location: String?,
            email: String?,
            bio: String?,
            twitterUsername: String?,
            followers: Int,
        ): GitHubUser = GitHubUser(
            id = id,
            login = login,
            type = type,
            name = name?.takeIf { it.isNotBlank() } ?: login,
            company = company?.takeIf { it.isNotBlank() },
            blog = blog?.takeIf { it.isNotBlank() },
            location = location?.takeIf { it.isNotBlank() },
            email = email?.takeIf { it.isNotBlank() },
            bio = bio?.takeIf { it.isNotBlank() },
            twitterUsername = twitterUsername?.takeIf { it.isNotBlank() },
            followers = followers,
        )
    }
}

/** The kohsuke client exposes no user-by-id lookup, so `GET /user/{id}` is called directly. */
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GitHubUserByIdResponse(
    val id: Long,
    val login: String,
    val type: String,
    val name: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val location: String? = null,
    val email: String? = null,
    val bio: String? = null,
    @field:JsonProperty("twitter_username")
    val twitterUsername: String? = null,
    val followers: Int = 0,
) {
    fun toModel(): GitHubUser = GitHubUser.of(
        id = id,
        login = login,
        type = type,
        name = name,
        company = company,
        blog = blog,
        location = location,
        email = email,
        bio = bio,
        twitterUsername = twitterUsername,
        followers = followers,
    )
}
