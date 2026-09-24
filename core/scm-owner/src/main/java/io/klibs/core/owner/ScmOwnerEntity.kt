package io.klibs.core.owner

import java.time.Instant

data class ScmOwnerEntity(
    val id: Int? = null,
    val nativeId: Long,
    val type: ScmOwnerType,

    val login: String,
    val name: String,
    val description: String?,

    val location: String?,
    val followers: Int,
    val company: String?,

    val homepage: String?,
    val twitterHandle: String?,
    val email: String?,

    val updatedAtTs: Instant
) {
    val idNotNull: Int get() = requireNotNull(id)

    fun getAvatarUrl(): String {
        return "https://avatars.githubusercontent.com/u/${this.nativeId}?v=4"
    }
}
