package io.klibs.integration.mcp

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.search.dto.validation.SearchLimits.MAX_QUERY_LENGTH
import io.klibs.core.search.repository.ProjectSearchRepository
import io.klibs.core.search.service.SearchService
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.MAX_PACKAGES_PER_PROJECT
import io.klibs.integration.mcp.service.McpProjectSearchService
import io.klibs.integration.mcp.tool.McpProjectSearchTool
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

/**
 * Runs the MCP search tool against real, validation-proxied [McpProjectSearchService] and [SearchService]
 * beans to make sure oversized or invalid input is rejected before any search is executed.
 */
@SpringJUnitConfig(McpProjectSearchToolValidationTest.Config::class)
class McpProjectSearchToolValidationTest {

    @Autowired
    private lateinit var uut: McpProjectSearchTool

    @Autowired
    private lateinit var projectSearchRepository: ProjectSearchRepository

    @BeforeEach
    fun setUp() {
        clearMocks(projectSearchRepository)
        every { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `accepts query at the length limit`() {
        assertDoesNotThrow { uut.searchProjects(query = "y".repeat(MAX_QUERY_LENGTH)) }
        verify(exactly = 1) { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `rejects oversized query without executing the search`() {
        assertThrows<ConstraintViolationException> { uut.searchProjects(query = "y".repeat(MAX_QUERY_LENGTH + 1)) }
        assertSearchNotExecuted()
    }

    @Test
    fun `rejects Unknown target group without executing the search`() {
        val filters = listOf(mapOf(TargetGroup.Unknown to emptySet<String>()))
        assertThrows<ConstraintViolationException> {
            uut.searchProjects(query = "kotlin", targetGroupFilters = filters)
        }
        assertSearchNotExecuted()
    }

    @Test
    fun `rejects maxPackagesPerProject below 1`() {
        assertThrows<ConstraintViolationException> { uut.searchProjects(query = "kotlin", maxPackagesPerProject = 0) }
        assertSearchNotExecuted()
    }

    @Test
    fun `rejects maxPackagesPerProject above the limit`() {
        assertThrows<ConstraintViolationException> {
            uut.searchProjects(query = "kotlin", maxPackagesPerProject = MAX_PACKAGES_PER_PROJECT + 1)
        }
        assertSearchNotExecuted()
    }

    @Test
    fun `accepts maxPackagesPerProject at the limit`() {
        assertDoesNotThrow { uut.searchProjects(query = "kotlin", maxPackagesPerProject = MAX_PACKAGES_PER_PROJECT) }
    }

    private fun assertSearchNotExecuted() {
        verify(exactly = 0) { projectSearchRepository.find(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Configuration
    class Config {
        @Bean
        fun projectSearchRepository(): ProjectSearchRepository = mockk()

        @Bean
        fun searchService(projectSearchRepository: ProjectSearchRepository) = SearchService(
            projectSearchRepository = projectSearchRepository,
            packageSearchRepository = mockk(),
            projectIndexJdbc = mockk(),
            packageIndexJdbc = mockk(),
            applicationScope = mockk(),
        )

        @Bean
        fun mcpProjectSearchService(searchService: SearchService) =
            McpProjectSearchService(searchService, mockk<PackageService>())

        @Bean
        fun mcpProjectSearchTool(mcpProjectSearchService: McpProjectSearchService) =
            McpProjectSearchTool(mcpProjectSearchService, McpToolMapper())

        companion object {
            @Bean
            @JvmStatic
            fun methodValidationPostProcessor() = MethodValidationPostProcessor()
        }
    }
}
