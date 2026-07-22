package io.klibs.integration.mcp

import io.klibs.core.pckg.model.PackageDetails
import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpPackageService
import io.klibs.integration.mcp.tool.McpPackageTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class McpPackageToolTest {

    private val mcpPackageService = mock<McpPackageService>()
    private val mcpToolMapper = McpToolMapper()
    private val mcpPackageTool = McpPackageTool(mcpPackageService, mcpToolMapper)

    @Test
    fun `getLatestVersion delegates to McpPackageService and maps result`() {
        val groupId = "org.jetbrains.kotlinx"
        val artifactId = "kotlinx-coroutines-core"
        val serviceResult = McpPackageLatestVersionResultDto(
            groupId = groupId,
            artifactId = artifactId,
            latestVersion = null,
            latestStableVersion = null,
            packageFound = false
        )

        whenever(mcpPackageService.getLatestVersion(groupId, artifactId))
            .thenReturn(serviceResult)

        val result = mcpPackageTool.getLatestVersion(groupId, artifactId)

        assertEquals(groupId, result.groupId)
        assertEquals(artifactId, result.artifactId)
        assertEquals(false, result.packageFound)
        assertEquals(null, result.latestVersion)
        assertEquals(null, result.latestStableVersion)
        verify(mcpPackageService).getLatestVersion(groupId, artifactId)
    }

    @Test
    fun `getLatestVersion maps package version details`() {
        val groupId = "org.jetbrains.kotlinx"
        val artifactId = "kotlinx-coroutines-core"
        val serviceResult = McpPackageLatestVersionResultDto(
            groupId = groupId,
            artifactId = artifactId,
            latestVersion = packageDetails("2.0.0-RC"),
            latestStableVersion = packageDetails("1.10.2"),
            packageFound = true
        )

        whenever(mcpPackageService.getLatestVersion(groupId, artifactId))
            .thenReturn(serviceResult)

        val result = mcpPackageTool.getLatestVersion(groupId, artifactId)

        assertEquals("2.0.0-RC", result.latestVersion?.version)
        assertEquals("Gradle", result.latestVersion?.buildTool)
        assertEquals("8.10", result.latestVersion?.buildToolVersion)
        assertEquals("2.1.0", result.latestVersion?.kotlinVersion)
        assertEquals("1.10.2", result.latestStableVersion?.version)
        assertEquals(true, result.packageFound)
    }

    private fun packageDetails(version: String) = PackageDetails(
        id = 1L,
        projectId = 1,
        groupId = "org.jetbrains.kotlinx",
        artifactId = "kotlinx-coroutines-core",
        version = version,
        releasedAt = Instant.EPOCH,
        description = null,
        targets = emptyList(),
        licenses = emptyList(),
        developers = emptyList(),
        buildTool = "Gradle",
        buildToolVersion = "8.10",
        kotlinVersion = "2.1.0",
        url = null,
        scmUrl = null
    )
}
