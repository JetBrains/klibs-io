package io.klibs.integration.mcp

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpProjectSearchService
import io.klibs.integration.mcp.tool.McpProjectSearchTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class McpProjectSearchToolTest {

    private val mcpProjectSearchService = mock<McpProjectSearchService>()
    private val mcpToolMapper = Mappers.getMapper(McpToolMapper::class.java)
    private val uut = McpProjectSearchTool(mcpProjectSearchService, mcpToolMapper)

    @Test
    fun `searchProjects passes all parameters to service`() {
        val serviceResponse = McpProjectSearchResultDto(projects = emptyList())
        val targetFilters = mapOf(TargetGroup.JVM to setOf("11", "17"))

        whenever(
            mcpProjectSearchService.mcpProjectSearch(
                query = "kotlin",
                platforms = listOf(PackagePlatform.JVM, PackagePlatform.NATIVE),
                targetFilters = targetFilters,
            )
        ).thenReturn(serviceResponse)

        val result = uut.searchProjects(
            query = "kotlin",
            platforms = listOf("jvm", "native"),
            targetFilters = targetFilters,
        )

        assertTrue(result.projects.isEmpty())
        verify(mcpProjectSearchService).mcpProjectSearch(
            query = "kotlin",
            platforms = listOf(PackagePlatform.JVM, PackagePlatform.NATIVE),
            targetFilters = targetFilters,
        )
    }

    @Test
    fun `searchProjects maps latest and latest stable versions onto packages`() {
        val packageOverview = PackageOverview(
            id = 1L,
            groupId = "io.github.kstatemachine",
            artifactId = "kstatemachine-core",
            version = "0.32.0-alpha",
            latestStableVersion = "0.31.1",
            releasedAt = Instant.now(),
            description = "KStateMachine core module",
            targets = listOf(PackageTarget(PackagePlatform.COMMON, null))
        )
        val serviceResponse = McpProjectSearchResultDto(
            projects = listOf(
                McpProjectSearchResultDto.ProjectInfoDto(
                    project = searchProjectResult(),
                    packages = listOf(packageOverview),
                    readme = null
                )
            )
        )

        whenever(mcpProjectSearchService.mcpProjectSearch(query = "state machine", platforms = emptyList(), targetFilters = emptyMap()))
            .thenReturn(serviceResponse)

        val result = uut.searchProjects(query = "state machine", platforms = emptyList(), targetFilters = emptyMap())

        val mappedPackage = result.projects.single().packages.single()
        assertEquals("0.32.0-alpha", mappedPackage.latestVersion)
        assertEquals("0.31.1", mappedPackage.latestStableVersion)
    }

    private fun searchProjectResult() = SearchProjectResult(
        id = 1,
        name = "kstatemachine",
        repoName = "kstatemachine",
        description = "Test project",
        vcsStars = 100,
        ownerType = ScmOwnerType.ORGANIZATION,
        ownerLogin = "KStateMachine",
        licenseName = "Apache-2.0",
        latestVersion = "0.32.0-alpha",
        latestVersionPublishedAt = Instant.now(),
        platforms = listOf(PackagePlatform.COMMON),
        targets = emptyList(),
        tags = emptyList(),
        markers = emptyList()
    )
}
