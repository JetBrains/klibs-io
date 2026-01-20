package io.klibs.app.api

import jakarta.validation.constraints.NotBlank

data class UpdateProjectDescriptionRequest(
    @field:NotBlank(message = "Project name cannot be blank")
    val projectName: String,
    @field:NotBlank(message = "Owner login cannot be blank")
    val ownerLogin: String,
    val description: String
)
