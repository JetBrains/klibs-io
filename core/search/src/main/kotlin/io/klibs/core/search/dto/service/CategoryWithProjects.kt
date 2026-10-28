package io.klibs.core.search.dto.service

import io.klibs.core.search.enums.CategoryProjectSelector
import io.klibs.core.search.dto.repository.SearchProjectResult

data class CategoryWithProjects(
    val categoryName: String,
    val categoryMarkers: List<String>,
    val selector: CategoryProjectSelector?,
    val projects: List<SearchProjectResult>,
)