package io.klibs.integration.mcp.dto.api

data class ProjectSearchResponse(
    val projects: List<ProjectSearchResult>
) {
    data class ProjectSearchResult(
        val projectName: String,
        val projectAuthor: String,
        val platforms: List<String>,
        val targets: List<String>,
        val packages: List<ProjectPackage>,
        val readme: String?,
    )

    data class ProjectPackage(
        val groupId: String,
        val artifactId: String,
        val description: String?,
    )
}
