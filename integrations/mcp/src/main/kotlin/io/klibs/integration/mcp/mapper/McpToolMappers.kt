package io.klibs.integration.mcp.mapper

import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.integration.mcp.dto.api.PackageLatestVersionResponse
import io.klibs.integration.mcp.dto.api.ProjectSearchResponse
import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper

@Component
class McpToolMapper {

    fun mapToLatestVersionResponse(result: McpPackageLatestVersionResultDto): PackageLatestVersionResponse =
        PackageLatestVersionMapper.map(result)

    fun mapToProjectSearchResponse(serviceResponse: McpProjectSearchResultDto): ProjectSearchResponse =
        ProjectSearchResponseMapper.map(serviceResponse)

}

private object PackageVersionMapper :
    ObjectMappie<PackageDetails, PackageLatestVersionResponse.PackageVersionResponse>()

private object PackageLatestVersionMapper :
    ObjectMappie<McpPackageLatestVersionResultDto, PackageLatestVersionResponse>() {

    override fun map(from: McpPackageLatestVersionResultDto): PackageLatestVersionResponse = mapping {
        to::latestVersion fromProperty from::latestVersion via PackageVersionMapper
        to::latestStableVersion fromProperty from::latestStableVersion via PackageVersionMapper
    }
}

private object ProjectPackageMapper : ObjectMappie<PackageOverview, ProjectSearchResponse.ProjectPackage>() {

    override fun map(from: PackageOverview): ProjectSearchResponse.ProjectPackage = mapping {
        to::latestVersion fromProperty from::version
    }
}

private object ProjectInfoMapper :
    ObjectMappie<McpProjectSearchResultDto.ProjectInfoDto, ProjectSearchResponse.ProjectSearchResult>() {

    override fun map(from: McpProjectSearchResultDto.ProjectInfoDto): ProjectSearchResponse.ProjectSearchResult = mapping {
        to::projectName fromProperty from.project::name
        to::projectAuthor fromProperty from.project::ownerLogin
        to::description fromProperty from.project::description
        to::platforms fromProperty from.project::platforms via IterableToListMapper(PackagePlatformMapper)
        to::targets fromProperty from.project::targets
        to::packages fromProperty from::packages via IterableToListMapper(ProjectPackageMapper)
        to::totalPackages fromProperty from::totalPackages
    }
}

private object ProjectSearchResponseMapper : ObjectMappie<McpProjectSearchResultDto, ProjectSearchResponse>() {

    override fun map(from: McpProjectSearchResultDto): ProjectSearchResponse = mapping {
        to::projects fromProperty from::projects via IterableToListMapper(ProjectInfoMapper)
    }
}

private object PackagePlatformMapper : ObjectMappie<PackagePlatform, String>() {

    override fun map(from: PackagePlatform): String = from.serializableName
}
