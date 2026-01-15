package io.klibs.core.owner

sealed interface ScmOwner {
    val id: Int
    val login: String
}

data class ScmOwnerAuthor(
    override val id: Int,
    override val login: String,

    val avatarUrl: String,

    val name: String,
    val description: String?,

    val location: String?,
    val followers: Int,
    val company: String?,

    val homepage: String?,
    val twitterHandle: String?,
    val email: String?,
) : ScmOwner

data class ScmOwnerOrganization(
    override val id: Int,
    override val login: String,

    val avatarUrl: String,

    val name: String,
    val description: String?,

    val homepage: String?,
    val twitterHandle: String?,
    val email: String?
) : ScmOwner
