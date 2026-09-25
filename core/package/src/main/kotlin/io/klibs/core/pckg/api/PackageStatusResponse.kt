package io.klibs.core.pckg.api

import io.klibs.core.pckg.enums.PackageProcessingStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageStatus",
    description = "Status of processing a package by klibs.io"
)
data class PackageStatusResponse (
    @field:Schema(
        description = "Group ID of the Maven artifact",
        example = "org.jetbrains.kotlinx"
    )
    val groupId: String,

    @field:Schema(
        description = "Artifact ID of the Maven artifact",
        example = "kotlinx-coroutines-core"
    )
    val artifactId: String,

    @field:Schema(
        description = "Version of the Maven artifact",
        example = "1.9.0-RC"
    )
    val version: String,

    @field:Schema(
        description = "Current status of processing the package",
        example = "QUEUED"
    )
    val status: PackageProcessingStatus,

    @field:Schema(
        description = "Human-readable explanation of the current status",
    )
    val statusDescription: String,
)
