package io.klibs.integration.mcp.configuration

import io.klibs.integration.mcp.mapper.McpToolMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MappingConfiguration {

    @Bean
    fun mcpToolMapper(): McpToolMapper = McpToolMapper()
}
