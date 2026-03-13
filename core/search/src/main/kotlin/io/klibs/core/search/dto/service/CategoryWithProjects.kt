package io.klibs.core.search.dto.service

import io.klibs.core.search.dto.repository.SearchProjectResult

data class CategoryWithProjects(
    val categoryName: String,
    val categoryMarkers: List<String>,
    val projects: List<SearchProjectResult>,
)