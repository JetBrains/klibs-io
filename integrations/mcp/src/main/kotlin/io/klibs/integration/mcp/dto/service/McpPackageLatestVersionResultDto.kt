package io.klibs.integration.mcp.dto.service

import io.klibs.core.pckg.model.PackageDetails

data class McpPackageLatestVersionResultDto(
    val groupId: String,
    val artifactId: String,
    val latestVersion: PackageDetails?,
    val latestStableVersion: PackageDetails?,
    val packageFound: Boolean
)
