package io.klibs.core.search.dto.api

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.validation.ValidTargetGroupValues
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "SearchPackagesRequest",
    description = "Request object for searching packages"
)
data class SearchPackagesRequest(
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
        example = """[{"JVM": ["11", "17"]}, {"AndroidJvm": [], "AndroidNative": []}]"""
    )
    @field:ValidTargetGroupValues
    val targetFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),

    @Schema(
        description = "Login of the owner",
        example = "Kotlin-Multiplatform-Foundation"
    )
    val owner: String? = null,
    @Schema(
        description = "Sorting order",
        allowableValues = ["most-stars", "relevance"],
        defaultValue = "relevance"
    )
    val sortBy: String = "relevance"
)
