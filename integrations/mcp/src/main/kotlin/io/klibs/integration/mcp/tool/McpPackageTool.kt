package io.klibs.integration.mcp.tool

import io.klibs.integration.mcp.dto.api.PackageLatestVersionResponse
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpPackageService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class McpPackageTool(
    private val mcpPackageService: McpPackageService,
    private val mcpToolMapper: McpToolMapper
) {

    @Tool(
        description = """Returns the latest and latest stable versions for a given Kotlin Multiplatform package identified by groupId and artifactId.
            Response fields:
            - groupId: The groupId that was queried
            - artifactId: The artifactId that was queried
            - latestVersion: The latest version info (version, buildTool, buildToolVersion, kotlinVersion), or null if not found
            - latestStableVersion: The latest stable version info (version, buildTool, buildToolVersion, kotlinVersion), or null if not found
            - packageFound: Boolean flag indicating if latest or latest stable versions were found for the package"""
    )
    fun getLatestVersion(
        @ToolParam(description = "The Kotlin Multiplatform groupId of the package") groupId: String,
        @ToolParam(description = "The Kotlin Multiplatform artifactId of the package") artifactId: String
    ): PackageLatestVersionResponse {
        val result = mcpPackageService.getLatestVersion(groupId, artifactId)
        return mcpToolMapper.mapToLatestVersionResponse(result)
    }
}
