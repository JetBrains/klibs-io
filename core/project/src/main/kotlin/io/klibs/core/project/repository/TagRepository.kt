package io.klibs.core.project.repository

import io.klibs.core.project.tags.TagStatisticsDTO

interface TagRepository {
    fun getTagStatistics(limit: Int): TagStatisticsDTO

    fun getTagsByProjectId(projectId: Int): List<String>
}