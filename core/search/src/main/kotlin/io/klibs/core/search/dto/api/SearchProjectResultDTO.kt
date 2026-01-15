package io.klibs.core.search.dto.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "ProjectSearchResults",
    description = "Overview of a project that matches the search query"
)
data class SearchProjectResultDTO(
    @Schema(
        description = "Unique id of the project",
        example = "4"
    )
    val id: Int,

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
        description = "Link to the SCM, such as GitHub",
        example = "https://github.com/KStateMachine/kstatemachine"
    )
    val scmLink: String,

    @Schema(
        description = "SCM stars or any other similar metric.",
        example = "351"
    )
    val scmStars: Int,

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
        description = "The name of the license from SCM, can be displayed on frontend. Not guaranteed to be the same as package licenses",
        example = "Boost Software License 1.0"
    )
    val licenseName: String?,

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
        description = "Platforms supported by the project's packages. Predefined values.",
        allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"]
    )
    val platforms: List<String>,

    @Schema(
        description = "Tags associated with the project. Can be used for filtering or grouping",
        example = "[Compose UI, Jetpack Compose]"
    )
    val tags: List<String>,

    @Schema(
        description = "Markers associated with the project.",
        example = "[Compose UI, Jetpack Compose]"
    )
    val markers: List<String>,
)