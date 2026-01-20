package io.klibs.app.api

import jakarta.validation.constraints.NotBlank

/**
 * Request body for updating a package description
 */
data class UpdatePackageDescriptionRequest(
    @field:NotBlank(message = "Package groupId cannot be blank")
    val groupId: String,
    @field:NotBlank(message = "Package artifactId cannot be blank")
    val artifactId: String,
    @field:NotBlank(message = "Package version cannot be blank")
    val version: String,
    val description: String
)