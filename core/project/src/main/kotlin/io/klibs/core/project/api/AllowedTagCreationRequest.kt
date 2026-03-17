package io.klibs.core.project.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AllowedTagCreationRequest(
    @field:NotBlank("Name of the new tag cannot be blank")
    @field:Pattern("^[a-z0-9-]+$")
    val name: String,
    val definition: String?,
    val positiveCues: List<String> = emptyList(),
    val negativesCues: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
)
