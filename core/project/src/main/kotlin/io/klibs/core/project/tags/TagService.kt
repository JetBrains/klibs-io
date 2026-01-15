package io.klibs.core.project.tags

import io.klibs.core.project.repository.TagRepository
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository
) {
    fun getTagStatistics(limit: Int): TagStatisticsDTO {
        return tagRepository.getTagStatistics(limit)
    }
}