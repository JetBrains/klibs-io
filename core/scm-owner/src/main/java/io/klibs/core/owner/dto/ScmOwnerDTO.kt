package io.klibs.core.owner.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "Owner",
    description = "Owner details. Base parent type of OwnerOrganization (organization) and OwnerAuthor (author)"
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(name = "organization", value = ScmOwnerOrganizationDTO::class),
    JsonSubTypes.Type(name = "author", value = ScmOwnerAuthorDTO::class)
)
sealed interface ScmOwnerDTO {
    val id: Int
    val login: String
}
