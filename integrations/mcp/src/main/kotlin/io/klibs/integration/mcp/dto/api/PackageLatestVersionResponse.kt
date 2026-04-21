package io.klibs.integration.mcp.dto.api

data class PackageLatestVersionResponse(
    val groupId: String,
    val artifactId: String,
    val latestVersion: PackageVersionResponse?,
    val latestStableVersion: PackageVersionResponse?,
    /**
     * Flag that indicates if latest or latest stable versions were found for the package
     */
    val packageFound: Boolean
) {
    data class PackageVersionResponse(
        val version: String,
        val buildTool: String,
        val buildToolVersion: String,
        val kotlinVersion: String
    )
}