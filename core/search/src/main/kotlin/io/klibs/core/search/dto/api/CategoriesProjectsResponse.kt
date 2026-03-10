package io.klibs.core.search.dto.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "CategoriesProjectsResponse", description = "All categories with their top projects")
data class CategoriesProjectsResponse(
    val categories: List<CategoryWithProjectsDTO>,
)

@Schema(name = "CategoryWithProjects", description = "A single category with its projects")
data class CategoryWithProjectsDTO(
    val category: CategoryDTO,
    val projects: List<SearchProjectResultDTO>,
)

@Schema(name = "Category", description = "Category metadata")
data class CategoryDTO(
    @Schema(description = "Display name of the category", example = "Featured")
    val name: String,
    @Schema(description = "Marker types that belong to this category", example = "[\"FEATURED\"]")
    val markers: List<String>,
)
