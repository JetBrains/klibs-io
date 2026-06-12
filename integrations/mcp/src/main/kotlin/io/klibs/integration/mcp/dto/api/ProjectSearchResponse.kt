package io.klibs.integration.mcp.dto.api

data class ProjectSearchResponse(
    val projects: List<ProjectSearchResult>
) {
    data class ProjectSearchResult(
        val projectName: String,
        val projectAuthor: String,
        val description: String?,
        val platforms: List<String>,
        val targets: List<String>,
        val packages: List<ProjectPackage>,
        val totalPackages: Int,
    )

    data class ProjectPackage(
        val groupId: String,
        val artifactId: String,
        val latestVersion: String,
        val latestStableVersion: String?,
        val description: String?,
    )
}
