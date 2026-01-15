package io.klibs.core.owner.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "OwnerOrganization",
    description = "Owner that is an organization. This is usually a profile of a company or a collective of people."
)
data class ScmOwnerOrganizationDTO(
    @Schema(
        description = "Unique id of the organization",
        example = "4"
    )
    override val id: Int,

    @Schema(
        description = "Organization's login (same as scm/github login)",
        example = "KStateMachine"
    )
    override val login: String,

    @Schema(
        description = "Organization's avatar URL",
        example = "https://avatars.githubusercontent.com/u/161740390?v=4"
    )
    val avatarUrl: String,

    @Schema(
        description = "Organization's name. Might or might not be the same as the login",
        example = "KStateMachine Inc"
    )
    val name: String,

    @Schema(
        description = "Organization's description, usually short",
        example = "KStateMachine is a powerful Kotlin Multiplatform library with clean DSL syntax for creating complex state machines and statecharts driven by Kotlin Coroutines."
    )
    val description: String?,

    @Schema(
        description = "Homepage link, may be absent",
        example = "https://kstatemachine.github.io/kstatemachine/"
    )
    val homepage: String?,

    @Schema(
        description = "Twitter (X) handle, without '@'. Not a link. May be absent",
        example = "StevenJeuris"
    )
    val twitterHandle: String?,

    @Schema(
        description = "Plain email, not a link. May be absent.",
        example = "lawyers@corp.com"
    )
    val email: String?
) : ScmOwnerDTO
