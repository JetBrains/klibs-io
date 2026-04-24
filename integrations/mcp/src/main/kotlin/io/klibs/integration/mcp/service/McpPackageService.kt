package io.klibs.integration.mcp.service

import io.klibs.core.pckg.service.PackageService
import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import org.springframework.stereotype.Service

@Service
class McpPackageService(
    private val packageService: PackageService
) {

    fun getLatestVersion(groupId: String, artifactId: String): McpPackageLatestVersionResultDto {
        val latestPackageVersion = packageService.getLatestPackageDetails(groupId, artifactId)
        val latestStablePackageVersion = packageService.getLatestStablePackageDetails(groupId, artifactId)

        return McpPackageLatestVersionResultDto(
            groupId = groupId,
            artifactId = artifactId,
            latestVersion = latestPackageVersion,
            latestStableVersion = latestStablePackageVersion,
            packageFound = latestPackageVersion != null || latestStablePackageVersion != null
        )
    }
}
