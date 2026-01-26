package io.klibs.core.pckg.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageDetails",
    description = "Full details of a package"
)
data class PackageDetailsResponse(
    @Schema(
        description = "Unique id of the package",
        example = "6"
    )
    val id: Long,

    @Schema(
        description = "Unique id of the parent project. A package is not guaranteed to belong to a project, nullable",
        example = "4"
    )
    val projectId: Int?,

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

    @Deprecated("Use artifactId instead")
    @Schema(
        description = "Name of the package, DEPRECATED: Use artifactId instead",
        example = "kstatemachine",
        deprecated = true
    )
    val name: String?,

    @Schema(
        description = "Description of the package specified by the published, some arbitrary text",
        example = "The main artifact of the library"
    )
    val description: String?,

    @Schema(description = "Platforms and targets supported by this package")
    val targets: List<PackageTargetResponse>,

    @Schema(description = "Licenses of this package. Usually one, but can be more")
    val licenses: List<OptionalLinkResponse>,

    @Schema(description = "Developers of this package. Usually empty, but can be multiple")
    val developers: List<OptionalLinkResponse>,

    @Schema(
        description = "Build tool (with the version) that was used to build this package",
        example = "Gradle 7.1.6"
    )
    val buildTool: String,

    @Schema(
        description = "Kotlin version that was used to build this package",
        example = "2.0.0"
    )
    val kotlinVersion: String,

    @Schema(
        description = "Link to the package's homepage, set by the user and unverified, nullable",
        example = "https://kstatemachine.github.io/kstatemachine/"
    )
    val linkHomepage: String?,

    @Schema(
        description = "Link to the SCM repository, nullable",
        example = "https://github.com/KStateMachine/kstatemachine"
    )
    val linkScm: String?,

    @Schema(
        description = "Link to the artifact's files, such as on Maven Central",
        example = "https://repo1.maven.org/maven2/io/github/nsk90/kstatemachine/0.31.1/"
    )
    val linkFiles: String
)

