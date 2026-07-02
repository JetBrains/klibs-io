package io.klibs.core.search.dto.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(
    name = "SearchSimilarProjectsRequest",
    description = "Request object for semantic project search by README embedding"
)
data class SearchSimilarProjectsRequest(
    @Schema(
        description = "Free text query that is embedded and matched against project README embeddings",
        example = "coroutine based state machine library"
    )
    @field:NotBlank(message = "Query must not be blank")
    val query: String,
)
