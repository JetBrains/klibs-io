package io.klibs.core.project.mapper

import io.klibs.core.project.api.AllAllowedTagsResponse
import io.klibs.core.project.api.AllowedTagCreationRequest
import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.entity.AllowedTagEntity
import org.springframework.stereotype.Component

@Component
class AllowedTagMapper {

    fun mapAllowedTagCreationRequestToDto(request: AllowedTagCreationRequest): AllowedTag =
        AllowedTag(
            name = request.name,
            definition = request.definition,
            positiveCues = request.positiveCues,
            negativesCues = request.negativesCues,
            synonyms = request.synonyms
        )

    fun mapToAllAllowedTagsResponse(allowedTag: List<AllowedTag>): AllAllowedTagsResponse {
        return AllAllowedTagsResponse(
            tags = allowedTag.map { AllAllowedTagsResponse.AllowedTagResponseDto(it.name, it.definition) }
        )
    }

    fun mapEntityListToDtoList(allowedTag: List<AllowedTagEntity>) = allowedTag.map { mapEntityToDto(it) }

    fun mapEntityToDto(allowedTag: AllowedTagEntity) =
        AllowedTag(
            name = allowedTag.name,
            definition = allowedTag.definition,
            positiveCues = allowedTag.positiveCues,
            negativesCues = allowedTag.negativesCues,
            synonyms = allowedTag.synonyms
        )

    fun mapDtoToEntity(allowedTag: AllowedTag) =
        AllowedTagEntity(
            name = allowedTag.name,
            definition = allowedTag.definition,
            positiveCues = allowedTag.positiveCues,
            synonyms = allowedTag.synonyms,
            negativesCues = allowedTag.negativesCues
        )
}