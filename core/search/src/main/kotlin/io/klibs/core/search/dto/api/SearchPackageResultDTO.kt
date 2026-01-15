package io.klibs.core.search.dto.api

import io.klibs.core.pckg.model.TargetGroup
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageSearchResults",
    description = "Overview of a package that matches the search query"
)
data class SearchPackageResultDTO(
    @Schema(
        description = "Group ID of the package",
        example = "io.github.kstatemachine"
    )
    val groupId: String,

    @Schema(
        description = "Artifact ID of the package",
        example = "kstatemachine"
    )
    val artifactId: String,

    @Schema(
        description = "Package's description. Can be null",
        example = "KStateMachine is a powerful Kotlin Multiplatform library with clean DSL syntax for creating complex state machines and statecharts driven by Kotlin Coroutines."
    )
    val description: String?,

    @Schema(
        description = "Link to the SCM, such as GitHub",
        example = "https://github.com/KStateMachine/kstatemachine"
    )
    val scmLink: String,

    @Schema(
        description = "Owner's type. Author means an individual contributor (personal profile).",
        example = "organization",
        allowableValues = ["organization", "author"]
    )
    val ownerType: String,

    @Schema(
        description = "Unique login of the owner, regardless of type",
        example = "KStateMachine"
    )
    val ownerLogin: String,

    @Schema(
        description = "The name of the license",
        example = "Boost Software License 1.0"
    )
    val licenseName: String?,

    @Schema(
        description = "Latest version of the package",
        example = "0.31.1"
    )
    val latestVersion: String,

    @Schema(
        description = "Epoch millis of when the package was released",
        example = "1725375720000"
    )
    val releaseTsMillis: Long,

    @Schema(
        description = "Platforms supported by the package. Predefined values.",
        allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"]
    )
    val platforms: List<String>,

    @Schema(
        description = "Targets supported by the package. Map where keys are target groups (e.g. 'JVM', 'IOS') and values are sets of specific targets within that group.",
        type = "object",
        example = """{"JVM": ["11", "17"], "IOS": ["ios_arm64", "ios_x64"]}"""
    )
    val targets: Map<TargetGroup, Set<String>>
)
