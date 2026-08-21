package io.klibs.app.api

import jakarta.validation.constraints.NotBlank

data class PluginNotifierIndexingRequest(
    @field:NotBlank(message = "Group ID cannot be blank")
    val groupId: String,
    @field:NotBlank(message = "Artifact ID cannot be blank")
    val artifactId: String,
    @field:NotBlank(message = "Version cannot be blank")
    val version: String,
)
