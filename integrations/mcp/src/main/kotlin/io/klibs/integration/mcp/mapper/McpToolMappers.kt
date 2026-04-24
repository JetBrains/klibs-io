package io.klibs.integration.mcp.mapper

import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.integration.mcp.configuration.SpringMappingConfiguration
import io.klibs.integration.mcp.dto.api.PackageLatestVersionResponse
import io.klibs.integration.mcp.dto.api.ProjectSearchResponse
import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = SpringMappingConfiguration::class)
interface McpToolMapper {

    fun mapPackageDetailsToPacakgeVersionResponse(
        packageDetails: PackageDetails
    ): PackageLatestVersionResponse.PackageVersionResponse

    @Mapping(source = "latestVersion", target = "latestVersion")
    @Mapping(source = "latestStableVersion", target = "latestStableVersion")
    fun mapToLatestVersionResponse(result: McpPackageLatestVersionResultDto): PackageLatestVersionResponse

    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "project.ownerLogin", target = "projectAuthor")
    @Mapping(source = "project.targets", target = "targets")
    @Mapping(source = "packages", target = "packages")
    @Mapping(source = "readme", target = "readme")
    @Mapping(target = "platforms", qualifiedByName = ["mapPlatforms"])
    fun mapToProjectSearchResult(
        project: SearchProjectResult,
        packages: List<ProjectSearchResponse.ProjectPackage>,
        readme: String?
    ): ProjectSearchResponse.ProjectSearchResult

    fun mapPackageOverviewToProjectPackage(packageOverview: PackageOverview): ProjectSearchResponse.ProjectPackage

    fun mapToProjectSearchResponse(serviceResponse: McpProjectSearchResultDto): ProjectSearchResponse {
        val projectResults = serviceResponse.projects.map { serviceResult ->
            val mappedPackages = serviceResult.packages.map { mapPackageOverviewToProjectPackage(it) }
            mapToProjectSearchResult(
                serviceResult.project,
                mappedPackages,
                serviceResult.readme
            )
        }
        return ProjectSearchResponse(projects = projectResults)
    }

    @Named("mapPlatforms")
    fun mapPlatforms(platforms: List<PackagePlatform>): List<String> {
        return platforms.map { it.serializableName }
    }

}
