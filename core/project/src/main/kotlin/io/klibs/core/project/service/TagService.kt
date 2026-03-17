package io.klibs.core.project.service

import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.mapper.AllowedTagMapper
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.project.tags.TagStatisticsDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TagService(
    private val tagRepository: TagRepository,
    private val allowedProjectTagsRepository: AllowedProjectTagsRepository,
    private val allowedTagMapper: AllowedTagMapper
) {
    fun getTagStatistics(limit: Int): TagStatisticsDTO {
        return tagRepository.getTagStatistics(limit)
    }

    fun getAllowedProjectTags(): List<AllowedTag> {
        return allowedTagMapper.mapEntityListToDtoList(allowedProjectTagsRepository.findAll())
    }

    fun createAllowedTag(allowedTag: AllowedTag): AllowedTag {
        if (allowedProjectTagsRepository.existsById(allowedTag.name)) {
            throw IllegalArgumentException("Tag with name ${allowedTag.name} already exists")
        }
        val entity = allowedTagMapper.mapDtoToEntity(allowedTag)
        val saved = allowedProjectTagsRepository.save(entity)
        return allowedTagMapper.mapEntityToDto(saved)
    }

    fun deleteAllowedTag(name: String) {
        allowedProjectTagsRepository.deleteById(name)
    }
}