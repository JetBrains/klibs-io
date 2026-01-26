package io.klibs.core.pckg.controller

import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.api.OptionalLinkResponse
import io.klibs.core.pckg.api.PackageDetailsResponse
import io.klibs.core.pckg.api.PackageOverviewResponse
import io.klibs.core.pckg.api.PackageTargetResponse
import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/package")
@Tag(name = "Packages", description = "Information about packages")
class PackageController(
    private val packageService: PackageService
) {
    @Operation(
        summary = "Get full package information by its coordinates",
        description = "If you don't have the version, there's a different request without one"
    )
    @GetMapping("/{groupId}/{artifactId}/{version}/details")
    fun getPackageDetails(
        @PathVariable("groupId")
        @Parameter(
            description = "Group ID of the Maven artifact",
            example = "org.jetbrains.kotlinx"
        )
        groupId: String,

        @PathVariable("artifactId")
        @Parameter(
            description = "Artifact ID of the Maven artifact",
            example = "kotlinx-coroutines-core"
        )
        artifactId: String,

        @PathVariable("version")
        @Parameter(
            description = "Version of the Maven artifact",
            example = "1.9.0-RC"
        )
        version: String
    ): PackageDetailsResponse? {
        return packageService.getPackageDetails(
            groupId = groupId,
            artifactId = artifactId,
            version = version
        )?.toDTO()
    }

    @Operation(summary = "Get the full info of the latest version by the group id and the artifact id")
    @GetMapping("/{groupId}/{artifactId}/details")
    fun getLatestPackageDetails(
        @PathVariable("groupId")
        @Parameter(
            description = "Group ID of the Maven artifact",
            example = "org.jetbrains.kotlinx"
        )
        groupId: String,

        @PathVariable("artifactId")
        @Parameter(
            description = "Artifact ID of the Maven artifact",
            example = "kotlinx-coroutines-core"
        )
        artifactId: String
    ): PackageDetailsResponse? {
        return packageService.getLatestPackageDetails(
            groupId = groupId,
            artifactId = artifactId
        )?.toDTO()
    }

    @Operation(summary = "Get an overview of all versions of the requested artifact")
    @GetMapping("/{groupId}/{artifactId}/versions")
    fun getPackageVersions(
        @PathVariable("groupId")
        @Parameter(
            description = "Group ID of the Maven artifact",
            example = "org.jetbrains.kotlinx"
        )
        groupId: String,

        @PathVariable("artifactId")
        @Parameter(
            description = "Artifact ID of the Maven artifact",
            example = "kotlinx-coroutines-core"
        )
        artifactId: String
    ): List<PackageOverviewResponse> {
        return packageService.getPackages(
            groupId = groupId,
            artifactId = artifactId
        ).map { it.toDTO() }
    }

    @Operation(summary = "Get the latest versions of artifacts published under the given group id")
    @GetMapping("/{groupId}/artifacts")
    fun getGroupIdArtifacts(
        @PathVariable("groupId")
        @Parameter(
            description = "Group ID of the Maven artifact",
            example = "org.jetbrains.kotlinx"
        )
        groupId: String
    ): List<PackageOverviewResponse> {
        return packageService.getLatestPackagesByGroupId(groupId)
            .map { it.toDTO() }
    }

    @Operation(
        summary = "Get mapping of target groups to their respective targets",
        description = "Returns a mapping of each target group to its set of supported targets"
    )
    @GetMapping("/target-groups")
    fun getTargetGroupsMapping(): Map<String, List<String>> {
        return TargetGroup.entries.associate { targetGroup -> targetGroup.name to targetGroup.targets }
    }
}

private fun PackageDetails.toDTO(): PackageDetailsResponse {
    return PackageDetailsResponse(
        id = this.id,
        projectId = this.projectId,
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        releasedAtMillis = this.releasedAt.toEpochMilli(),
        name = this.artifactId,
        description = this.description,
        targets = this.targets.map { it.toDTO() },
        licenses = this.licenses.map { OptionalLinkResponse(title = it.name, url = it.url) },
        developers = this.developers.map { OptionalLinkResponse(title = it.name, url = it.url) },
        buildTool = "${this.buildTool} ${this.buildToolVersion}",
        kotlinVersion = this.kotlinVersion,
        linkHomepage = this.url,
        linkScm = this.scmUrl,
        linkFiles = this.getFilesUrl()
    )
}

fun PackageOverview.toDTO(): PackageOverviewResponse {
    return PackageOverviewResponse(
        id = this.id,
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        releasedAtMillis = this.releasedAt.toEpochMilli(),
        description = this.description,
        targets = this.targets.map { it.toDTO() }
    )
}

private fun PackageTarget.toDTO(): PackageTargetResponse {
    return PackageTargetResponse(
        platform = this.platform.serializableName,
        target = this.target
    )
}

internal fun PackageDetails.getFilesUrl(): String {
    val androidxPackage = """^androidx\..+$""".toRegex()

    if (androidxPackage matches groupId) {
        return "https://maven.google.com/web/index.html#${groupId}:${artifactId}:${version}"
    } else {
        val path = "$groupId.$artifactId".replace(".", "/")
        return "https://repo1.maven.org/maven2/$path/${version}/"
    }
}
