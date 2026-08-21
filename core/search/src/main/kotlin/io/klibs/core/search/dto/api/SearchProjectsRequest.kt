package io.klibs.core.search.dto.api

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.validation.ValidTargetGroupValues
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "SearchProjectsRequest",
    description = "Request object for searching projects"
)
data class SearchProjectsRequest(
    @Schema(
        description = "Arbitrary full text search query",
        example = "kotlin"
    )
    val query: String? = null,

    @Schema(
        description = "Filter by specific targets within platform groups. Each list item is a group of target-group " +
                "filters combined with OR; separate list items are combined with AND. Keys are target groups " +
                "(e.g. 'JVM', 'Android Native'), values are sets of specific targets within that group " +
                "(an empty set means any target in the group).",
        type = "array",
        example = """[{"JVM": ["11", "17"]}, {"IOS": [], "TvOS": []}]"""
    )
    @field:ValidTargetGroupValues
    val targetGroupFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),

    @Schema(
        description = "Login of the owner",
        example = "Kotlin"
    )
    val owner: String? = null,
    @Schema(
        description = "Sorting order",
        allowableValues = ["most-stars", "most-healthy", "relevance"],
        defaultValue = "relevance"
    )
    val sortBy: String = "relevance",
    @Schema(
        description = "Filter by tags",
        type = "array",
        example = "\"kotlin\", \"android\""
    )
    val tags: List<String> = emptyList(),
    @Schema(
        description = "Filter by markers",
        type = "array",
        example = "\"FEATURED_DEPRECATED\", \"GRANT_WINNER_2024\""
    )
    val markers: List<String> = emptyList()
)
