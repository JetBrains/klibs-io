package io.klibs.core.pckg.api

import io.klibs.core.pckg.model.TargetGroup
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageOverview",
    description = "Overview of a package, not its full details"
)
data class PackageOverviewResponse(
    @Schema(
        description = "Unique id of the package",
        example = "6"
    )
    val id: Long,

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
        description = "Epoch millis of when this package was published to the Maven repo",
        example = "1725375645000"
    )
    val releasedAtMillis: Long,

    @Schema(
        description = "Short description of the Maven artifact",
        example = "The AWS SDK for Kotlin client for AppRunner"
    )
    val description: String?,

    @Schema(description = "Platforms and targets supported by this package")
    val targets: List<PackageTargetResponse>,

    @Schema(
        description = "Targets supported by this package, grouped by target group. " +
            "Map where keys are target groups (e.g. 'JVM', 'IOS') and values are sets of specific targets within that group.",
        type = "object",
        example = """{"JVM": ["11", "17"], "IOS": ["ios_arm64", "ios_x64"]}"""
    )
    val targetGroups: Map<TargetGroup, Set<String>>
)
