package io.klibs.core.project.service

import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.mapper.AllowedTagMapper
import io.klibs.core.project.repository.AllowedProjectTagsRepository
import io.klibs.core.project.repository.TagRepository
import io.klibs.core.project.tags.TagStatisticsDTO
import org.springframework.stereotype.Service

@Service
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
}