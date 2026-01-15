package io.klibs.core.search.suggest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "WordSuggestions",
    description = "List of word suggestions for project queries"
)
data class WordSuggestionDTO(
    @Schema(
        description = "List of suggested words for project queries",
        example = "[\"aws-sdk-kotlin\",\"Kotlin\",\"Monadic-Kotlin\"]"
    )
    val keywords: List<String>
)