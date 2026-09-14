package io.klibs.app.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Target Maven package coordinates")
data class PackageCoordinatesDTO(
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
)
