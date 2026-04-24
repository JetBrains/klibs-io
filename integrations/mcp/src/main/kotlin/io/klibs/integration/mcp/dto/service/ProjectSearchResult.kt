package io.klibs.integration.mcp.dto.service

import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.search.dto.repository.SearchProjectResult

data class McpProjectSearchResultDto(
    val projects: List<ProjectInfoDto>
) {

    data class ProjectInfoDto(
        val project: SearchProjectResult,
        val packages: List<PackageOverview>,
        val readme: String?
    )
}
