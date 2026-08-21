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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class McpProjectSearchToolTest {

    private val mcpProjectSearchService = mock<McpProjectSearchService>()
    private val mcpToolMapper = McpToolMapper()
    private val uut = McpProjectSearchTool(mcpProjectSearchService, mcpToolMapper)

    @Test
    fun `searchProjects passes all parameters to service`() {
        val serviceResponse = McpProjectSearchResultDto(projects = emptyList())
        val targetFilters = listOf(mapOf(TargetGroup.JVM to setOf("11", "17")))

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
            targetGroupFilters = targetFilters,
        )

        assertTrue(result.projects.isEmpty())
        verify(mcpProjectSearchService).mcpProjectSearch(
            query = "kotlin",
            platforms = listOf(PackagePlatform.JVM, PackagePlatform.NATIVE),
            targetFilters = targetFilters,
        )
    }

    @Test
    fun `searchProjects rejects Unknown target group and does not call service`() {
        val filters = listOf(mapOf(TargetGroup.Unknown to emptySet<String>()))

        assertThrows(IllegalArgumentException::class.java) {
            uut.searchProjects(query = "kotlin", platforms = emptyList(), targetGroupFilters = filters)
        }

        verify(mcpProjectSearchService, never()).mcpProjectSearch(
            query = org.mockito.kotlin.any(),
            platforms = org.mockito.kotlin.any(),
            targetFilters = org.mockito.kotlin.any(),
            maxPackagesPerProject = org.mockito.kotlin.any(),
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
                    totalPackages = 1
                )
            )
        )

        whenever(mcpProjectSearchService.mcpProjectSearch(query = "state machine", platforms = emptyList(), targetFilters = emptyList()))
            .thenReturn(serviceResponse)

        val result = uut.searchProjects(query = "state machine", platforms = emptyList(), targetGroupFilters = emptyList())

        val mappedProject = result.projects.single()
        assertEquals("kstatemachine", mappedProject.projectName)
        assertEquals("KStateMachine", mappedProject.projectAuthor)
        assertEquals("Test project", mappedProject.description)
        assertEquals(listOf("common"), mappedProject.platforms)
        assertEquals(emptyList<String>(), mappedProject.targets)
        assertEquals(1, mappedProject.totalPackages)

        val mappedPackage = mappedProject.packages.single()
        assertEquals("io.github.kstatemachine", mappedPackage.groupId)
        assertEquals("kstatemachine-core", mappedPackage.artifactId)
        assertEquals("0.32.0-alpha", mappedPackage.latestVersion)
        assertEquals("0.31.1", mappedPackage.latestStableVersion)
        assertEquals("KStateMachine core module", mappedPackage.description)
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
