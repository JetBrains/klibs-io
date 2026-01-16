package io.klibs.app.api

/**
 * Request body for updating a package description
 */
data class UpdatePackageDescriptionRequest(
    val description: String
)