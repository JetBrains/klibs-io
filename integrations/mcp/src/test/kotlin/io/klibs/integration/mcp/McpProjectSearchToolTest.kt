package io.klibs.integration.mcp

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.McpProjectSearchService
import io.klibs.integration.mcp.tool.McpProjectSearchTool
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
}
