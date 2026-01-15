package io.klibs.core.owner.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "OwnerAuthor",
    description = "An individual contributor, someone's personal profile. Has more personal information."
)
data class ScmOwnerAuthorDTO(
    @Schema(
        description = "Unique id of the author",
        example = "4"
    )
    override val id: Int,

    @Schema(
        description = "Author's login (same as scm/github login)",
        example = "nsk90"
    )
    override val login: String,

    @Schema(
        description = "Author's avatar URL",
        example = "https://avatars.githubusercontent.com/u/17552113?v=4"
    )
    val avatarUrl: String,

    @Schema(
        description = "Author's name. Might or might not be the same as the login",
        example = "Mikhail Fedotov"
    )
    val name: String,

    @Schema(
        description = "Author's description, usually short",
        example = "Lead Android developer. Coding with Kotlin, Java, C++."
    )
    val description: String?,

    @Schema(
        description = "Location of the author. An unverified field from SCM. May be absent",
        example = "Russia"
    )
    val location: String?,

    @Schema(
        description = "How many SCM profile followers this author has. Never null",
        example = "15"
    )
    val followers: Int,

    @Schema(
        description = "The company this author works for. An unverified field from SCM. May be absent",
        example = "Alfa-bank"
    )
    val company: String?,

    @Schema(
        description = "Homepage link, may be absent",
        example = "https://github.com/nsk90"
    )
    val homepage: String?,

    @Schema(
        description = "Twitter (X) handle, without '@'. Not a link. May be absent",
        example = "nosik90"
    )
    val twitterHandle: String?,

    @Schema(
        description = "Plain email, not a link. May be absent.",
        example = "nosik90@gmail.com"
    )
    val email: String?,
) : ScmOwnerDTO
