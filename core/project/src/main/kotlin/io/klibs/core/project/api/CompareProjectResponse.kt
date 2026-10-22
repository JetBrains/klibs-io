package io.klibs.core.project.api

import io.klibs.core.pckg.model.TargetGroup
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Project information returned when comparing Kotlin Multiplatform libraries"
)
data class CompareProjectResponse(
    @field:Schema(
        description = "Login of the project's SCM owner",
        example = "JetBrains"
    )
    val ownerLogin: String,

    @field:Schema(
        description = "Name of the project",
        example = "compose-multiplatform"
    )
    val projectName: String,

    @field:Schema(
        description = "Project description from its SCM repository or generated metadata",
        example = "Facilitates sharing UIs across iOS, Android, Desktop, and Web using a declarative framework. Provides high-performance UI rendering, native APIs access, and desktop-specific extensions. Includes libraries for web UI creation."
    )
    val description: String?,

    @field:Schema(
        description = "Number of stars on the project's SCM repository",
        example = "19436"
    )
    val scmStars: Int,

    @field:Schema(
        description = "Name of the license reported by the project's SCM repository",
        example = "Apache License 2.0"
    )
    val licenseName: String?,

    @field:Schema(
        description = "Latest release version associated with the project",
        example = "1.12.2"
    )
    val latestReleaseVersion: String?,

    @field:Schema(
        description = "Epoch milliseconds when the latest release was published",
        example = "1725375720000"
    )
    val latestReleasePublishedAtMillis: Long?,

    @field:Schema(
        description = "Epoch milliseconds of the project's last activity",
        example = "1725375720000"
    )
    val lastActivityAtMillis: Long,

    @field:Schema(
        description = "Epoch milliseconds when the project was created in its SCM",
        example = "1725375720000"
    )
    val createdAtMillis: Long,

    @field:Schema(
        description = "Number of open issues in the project's SCM repository",
        example = "42"
    )
    val openIssues: Int?,

    @field:Schema(
        description = "Targets supported by the project's packages, grouped by target group. " +
            "Map where keys are target groups (e.g. 'JVM', 'IOS') and values are sets of specific targets within that group.",
        type = "object",
        example = """{"JVM": ["11", "17"], "IOS": ["ios_arm64", "ios_x64"]}"""
    )
    val targetGroups: Map<TargetGroup, Set<String>>,

    @field:Schema(
        description = "Tags associated with the project",
        example = "[Compose UI, Compose]"
    )
    val tags: List<String>,

    @field:Schema(
        description = "Markers associated with the project",
        example = "[FEATURED_DEPRECATED, GRANT_WINNER_2023]"
    )
    val markers: List<String>,

    @field:Schema(
        description = "Kotlin version used by the project's latest package, when available",
        example = "2.0.0"
    )
    val kotlinVersion: String?
)
