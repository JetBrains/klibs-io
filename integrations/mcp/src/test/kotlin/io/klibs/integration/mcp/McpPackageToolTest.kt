package io.klibs.integration.mcp

import io.klibs.integration.mcp.dto.service.McpPackageLatestVersionResultDto
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpPackageService
import io.klibs.integration.mcp.tool.McpPackageTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class McpPackageToolTest {

    private val mcpPackageService = mock<McpPackageService>()
    private val mcpToolMapper = Mappers.getMapper(McpToolMapper::class.java)
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
}
