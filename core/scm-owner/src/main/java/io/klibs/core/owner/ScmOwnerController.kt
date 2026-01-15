package io.klibs.core.owner

import io.klibs.core.owner.dto.ScmOwnerAuthorDTO
import io.klibs.core.owner.dto.ScmOwnerDTO
import io.klibs.core.owner.dto.ScmOwnerOrganizationDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/owner")
@Tag(name = "Owners", description = "Information about project owners")
class ScmOwnerController(
    private val ownerService: ScmOwnerService,
) {
    @Operation(summary = "Get owner details by their login")
    @GetMapping("/{login}/details")
    fun getOwner(
        @PathVariable("login")
        @Parameter(
            description = "Login of the owner (same as the scm/github login)",
            example = "Kotlin"
        )
        login: String
    ): ScmOwnerDTO? {
        return ownerService.getOwner(login)?.toDTO()
    }
}

private fun ScmOwner.toDTO(): ScmOwnerDTO {
    return when (this) {
        is ScmOwnerAuthor -> ScmOwnerAuthorDTO(
            id = this.id,
            login = this.login,
            avatarUrl = this.avatarUrl,
            name = this.name,
            description = this.description,
            location = this.location,
            followers = this.followers,
            company = this.company,
            homepage = this.homepage,
            twitterHandle = this.twitterHandle,
            email = this.email
        )

        is ScmOwnerOrganization -> ScmOwnerOrganizationDTO(
            id = this.id,
            login = this.login,
            avatarUrl = this.avatarUrl,
            name = this.name,
            description = this.description,
            homepage = this.homepage,
            twitterHandle = this.twitterHandle,
            email = this.email
        )
    }
}
