package io.klibs.core.project

import io.klibs.core.project.enums.MarkerType
import io.swagger.v3.oas.annotations.media.Schema


@Schema(
    name = "ProjectDetails",
    description = "Full information about a project. Usually used for dedicated pages or where all data is needed"
)
data class ProjectDetailsDTO(
    @Schema(
        description = "Unique id of the project",
        example = "4"
    )
    val id: Int,

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
        description = "Name of the project. Unique only on combination with owner's login",
        example = "kstatemachine"
    )
    val name: String,

    @Schema(
        description = "Project's description. Can be AI generated or come from the SCM repo. Nullable",
        example = "KStateMachine is a powerful Kotlin Multiplatform library with clean DSL syntax for creating complex state machines and statecharts driven by Kotlin Coroutines."
    )
    val description: String?,

    @Schema(
        description = "Platforms supported by the project's packages. Predefined values.",
        allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"]
    )
    val platforms: List<String>,

    @Schema(
        description = "Latest version of the project. Not guaranteed to be the same as package versions",
        example = "0.31.1"
    )
    val latestReleaseVersion: String?,

    @Schema(
        description = "Epoch millis of when the latest release was published",
        example = "1725375720000"
    )
    val latestReleasePublishedAtMillis: Long?,

    @Schema(
        description = "Some arbitrary user link, unverified",
        example = "https://google.com"
    )
    val linkHomepage: String?,

    @Schema(
        description = "Link to the SCM, such as GitHub, if any",
        example = "https://github.com/KStateMachine/kstatemachine"
    )
    val linkScm: String?,

    @Schema(
        description = "Link to GitHub Pages, if any",
        example = "https://kotlin.github.io/dokka"
    )
    val linkGitHubPages: String?,

    @Schema(
        description = "Link to the issue tracker, if any",
        example = "https://github.com/KStateMachine/kstatemachine/issues"
    )
    val linkIssues: String?,

    @Schema(
        description = "Link to the wiki, if any",
        example = "https://github.com/KStateMachine/kstatemachine/wiki"
    )
    val linkWiki: String?,

    @Schema(
        description = "SCM stars or any other similar metric.",
        example = "351"
    )
    val scmStars: Int,

    @Schema(
        description = "Epoch millis of when the this project was first created. Same as SCM creation date.",
        example = "1725375720000"
    )
    val createdAtMillis: Long,

    @Schema(
        description = "Number of open issues. Nullable, only present if 'linkIssues' is not null.",
        example = "351"
    )
    val openIssues: Int?,

    @Schema(
        description = "The name of the license from SCM, can be displayed on frontend. Not guaranteed to be the same as package licenses",
        example = "Boost Software License 1.0"
    )
    val licenseName: String?,

    @Schema(
        description = "Epoch millis of last activity within this project. Same as last activity of SCM repo",
        example = "1725375720000"
    )
    val lastActivityAtMillis: Long,

    @Schema(
        description = "Epoch millis of when this project's information was last updated. Usually once a day",
        example = "1725375720000"
    )
    val updatedAtMillis: Long,

    @Schema(
        description = "Tags associated with the project. Can be used for filtering or grouping",
        example = "[Compose UI, Jetpack Compose]"
    )
    val tags: List<String>,


    @Schema(
        description = "Markers associated with the project",
        example = "[FEATURED, GRANT_WINNER_2023, GRANT_WINNER_2024]"
    )
    val markers: List<MarkerType>
)
