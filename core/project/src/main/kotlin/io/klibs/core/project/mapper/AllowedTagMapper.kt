package io.klibs.core.project.mapper

import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.entity.AllowedTagEntity
import org.springframework.stereotype.Component

@Component
class AllowedTagMapper {

    fun mapEntityListToDtoList(allowedTag: List<AllowedTagEntity>) = allowedTag.map { mapEntityToDto(it) }

    fun mapEntityToDto(allowedTag: AllowedTagEntity) =
        AllowedTag(
            name = allowedTag.name,
            definition = allowedTag.definition,
            positiveCues = allowedTag.positiveCues,
            negativesCues = allowedTag.negativesCues,
            synonyms = allowedTag.synonyms
        )
}