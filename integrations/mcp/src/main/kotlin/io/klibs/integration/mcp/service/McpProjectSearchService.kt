package io.klibs.integration.mcp.service

import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.service.SearchService
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import org.springframework.stereotype.Service

@Service
class McpProjectSearchService(
    private val searchService: SearchService,
    private val packageService: PackageService,
) {
    private companion object {
        private const val SEARCH_RESULTS_LIMIT = 5
        private const val DEFAULT_MAX_PACKAGES_PER_PROJECT = 10
    }

    fun mcpProjectSearch(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        maxPackagesPerProject: Int = DEFAULT_MAX_PACKAGES_PER_PROJECT,
    ): McpProjectSearchResultDto {
        val searchResults = searchService.search(
            query = query,
            platforms = platforms,
            targetFilters = targetFilters,
            ownerLogin = null,
            sort = SearchSort.RELEVANCY,
            markers = emptyList(),
            tags = emptyList(),
            page = 1,
            limit = SEARCH_RESULTS_LIMIT
        )

        val projectResults = searchResults.map { project ->
            val allPackages = packageService.getLatestPackagesByProjectId(project.id)
            val packages = allPackages
                .sortedWith(compareByDescending<PackageOverview> { it.dependentCount }.thenByDescending { it.releasedAt })
                .take(maxPackagesPerProject)

            McpProjectSearchResultDto.ProjectInfoDto(
                project = project,
                packages = packages,
                totalPackages = allPackages.size
            )
        }

        return McpProjectSearchResultDto(projects = projectResults)
    }
}
