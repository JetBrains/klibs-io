package io.klibs.app.api

import jakarta.validation.constraints.NotBlank

data class UpdateProjectTagsRequest(
    @field:NotBlank
    val projectName: String,
    @field:NotBlank
    val ownerLogin: String,
    val tags: List<String>
)
