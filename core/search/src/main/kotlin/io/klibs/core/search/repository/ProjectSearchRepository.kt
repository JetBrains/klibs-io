package io.klibs.core.search.repository

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.Category
import io.klibs.core.search.dto.repository.SearchProjectResult

interface ProjectSearchRepository {

    fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int
    ): List<SearchProjectResult>

    fun findCategoriesWithProjects(limit: Int): Map<Category, List<SearchProjectResult>>

    fun refreshIndex()
}