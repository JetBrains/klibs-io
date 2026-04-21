package io.klibs.integration.mcp.service

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.readme.service.ReadmeService
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.service.SearchService
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import org.springframework.stereotype.Service

@Service
class McpProjectSearchService(
    private val searchService: SearchService,
    private val packageService: PackageService,
    private val readmeService: ReadmeService
) {
    private companion object {
        private const val SEARCH_RESULTS_LIMIT = 5
        private const val MAX_PACKAGES_PER_PROJECT = 200
    }

    fun mcpProjectSearch(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
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
            val packages = packageService.getLatestPackagesByProjectId(project.id)
                .take(MAX_PACKAGES_PER_PROJECT)

            val readme = readmeService.readReadmeMd(
                ReadmeService.ProjectInfo(
                    id = project.id,
                    scmRepositoryId = null,
                    name = project.name,
                    ownerLogin = project.ownerLogin
                )
            )

            McpProjectSearchResultDto.ProjectInfoDto(
                project = project,
                packages = packages,
                readme = readme
            )
        }

        return McpProjectSearchResultDto(projects = projectResults)
    }
}
