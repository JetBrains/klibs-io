package io.klibs.core.search.dto.api

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKERS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_OWNER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_QUERY_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_SORT_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAGS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAG_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TARGET_GROUP_FILTERS
import io.klibs.core.search.dto.validation.ValidTargetGroupValues
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
    name = "SearchProjectsRequest",
    description = "Request object for searching projects"
)
data class SearchProjectsRequest(
    @Schema(
        description = "Arbitrary full text search query",
        example = "kotlin"
    )
    @field:Size(max = MAX_QUERY_LENGTH)
    val query: String? = null,

    @Schema(
        description = "Filter by specific targets within platform groups. Each list item is a group of target-group " +
                "filters combined with OR; separate list items are combined with AND. Keys are target groups " +
                "(e.g. 'JVM', 'Android Native'), values are sets of specific targets within that group " +
                "(an empty set means any target in the group).",
        type = "array",
        example = """[{"JVM": ["11", "17"]}, {"IOS": [], "TvOS": []}]"""
    )
    @field:Size(max = MAX_TARGET_GROUP_FILTERS)
    @field:ValidTargetGroupValues
    val targetGroupFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),

    @Schema(
        description = "Login of the owner",
        example = "Kotlin"
    )
    @field:Size(max = MAX_OWNER_LENGTH)
    val owner: String? = null,
    @Schema(
        description = "Sorting order",
        allowableValues = ["most-stars", "most-healthy", "relevance"],
        defaultValue = "relevance"
    )
    @field:Size(max = MAX_SORT_LENGTH)
    val sortBy: String = "relevance",
    @Schema(
        description = "Filter by tags",
        type = "array",
        example = "\"kotlin\", \"android\""
    )
    @field:Size(max = MAX_TAGS)
    val tags: List<@Size(max = MAX_TAG_LENGTH) String> = emptyList(),
    @Schema(
        description = "Filter by markers",
        type = "array",
        example = "\"FEATURED_DEPRECATED\", \"GRANT_WINNER_2024\""
    )
    @field:Size(max = MAX_MARKERS)
    val markers: List<@Size(max = MAX_MARKER_LENGTH) String> = emptyList()
)
