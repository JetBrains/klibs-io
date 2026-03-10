package io.klibs.core.search

import io.klibs.core.search.dto.api.CategoriesProjectsResponse
import io.klibs.core.search.dto.api.CategoryDTO
import io.klibs.core.search.dto.api.CategoryWithProjectsDTO
import io.klibs.core.search.dto.api.toDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
@Tag(name = "Categories", description = "Browse projects by categories")
@Validated
class CategoryController(
    private val searchService: SearchService,
) {

    @Operation(summary = "Get projects grouped by categories")
    @GetMapping("/projects")
    fun getProjectsByCategories(
        @RequestParam("limit", required = false, defaultValue = "7")
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 50, message = "Limit must be <= 50")
        limit: Int,
    ): CategoriesProjectsResponse {
        val results = searchService.searchByCategories(limit)

        return CategoriesProjectsResponse(
            categories = results.map { (category, projects) ->
                CategoryWithProjectsDTO(
                    category = CategoryDTO(name = category.name, markers = category.markers),
                    projects = projects.map { it.toDTO() }
                )
            }
        )
    }
}
