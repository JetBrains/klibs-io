package io.klibs.core.pckg.api

import io.klibs.core.pckg.enums.PackageProcessingStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageStatus",
    description = "Status of processing a package by klibs.io"
)
data class PackageStatusResponse (
    @Schema(
        description = "Group ID of the Maven artifact",
        example = "io.github.nsk90"
    )
    val groupId: String,

    @Schema(
        description = "Artifact ID of the Maven artifact",
        example = "kstatemachine"
    )
    val artifactId: String,

    @Schema(
        description = "Version of the Maven artifact",
        example = "0.31.1"
    )
    val version: String,

    @Schema(
        description = "Current status of processing the package",
        example = "QUEUED"
    )
    val status: PackageProcessingStatus,

    @Schema(
        description = "Human-readable explanation of the current status",
    )
    val statusDescription: String,
)
