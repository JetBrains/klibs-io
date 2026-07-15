package io.klibs.integration.mcp.mapper

import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.integration.mcp.dto.api.PackageLatestVersionResponse
import io.klibs.integration.mcp.dto.api.ProjectSearchResponse
import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto

class McpToolMapper {

    fun mapPackageDetailsToPackageVersionResponse(
        packageDetails: PackageDetails
    ): PackageLatestVersionResponse.PackageVersionResponse {
        return PackageLatestVersionResponse.PackageVersionResponse(
            version = packageDetails.version,
            buildTool = packageDetails.buildTool,
            buildToolVersion = packageDetails.buildToolVersion,
            kotlinVersion = packageDetails.kotlinVersion
        )
    }

    fun mapToLatestVersionResponse(result: McpPackageLatestVersionResultDto): PackageLatestVersionResponse {
        return PackageLatestVersionResponse(
            groupId = result.groupId,
            artifactId = result.artifactId,
            latestVersion = result.latestVersion?.let(::mapPackageDetailsToPackageVersionResponse),
            latestStableVersion = result.latestStableVersion?.let(::mapPackageDetailsToPackageVersionResponse),
            packageFound = result.packageFound
        )
    }

    fun mapToProjectSearchResult(
        project: SearchProjectResult,
        packages: List<ProjectSearchResponse.ProjectPackage>,
        totalPackages: Int
    ): ProjectSearchResponse.ProjectSearchResult {
        return ProjectSearchResponse.ProjectSearchResult(
            projectName = project.name,
            projectAuthor = project.ownerLogin,
            description = project.description,
            platforms = mapPlatforms(project.platforms),
            targets = project.targets,
            packages = packages,
            totalPackages = totalPackages
        )
    }

    fun mapPackageOverviewToProjectPackage(packageOverview: PackageOverview): ProjectSearchResponse.ProjectPackage {
        return ProjectSearchResponse.ProjectPackage(
            groupId = packageOverview.groupId,
            artifactId = packageOverview.artifactId,
            latestVersion = packageOverview.version,
            latestStableVersion = packageOverview.latestStableVersion,
            description = packageOverview.description
        )
    }

    fun mapToProjectSearchResponse(serviceResponse: McpProjectSearchResultDto): ProjectSearchResponse {
        val projectResults = serviceResponse.projects.map { serviceResult ->
            val mappedPackages = serviceResult.packages.map(::mapPackageOverviewToProjectPackage)
            mapToProjectSearchResult(
                serviceResult.project,
                mappedPackages,
                serviceResult.totalPackages
            )
        }
        return ProjectSearchResponse(projects = projectResults)
    }

    fun mapPlatforms(platforms: List<PackagePlatform>): List<String> {
        return platforms.map { it.serializableName }
    }
}
