package io.klibs.integration.mcp.configuration

import io.klibs.integration.mcp.tool.McpPackageTool
import io.klibs.integration.mcp.tool.McpProjectSearchTool
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpServerConfiguration {

    @Bean
    fun mcpPackageTools(
        mcpPackageTool: McpPackageTool,
        mcpProjectSearchTool: McpProjectSearchTool
    ): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(mcpPackageTool, mcpProjectSearchTool)
            .build()
    }
}