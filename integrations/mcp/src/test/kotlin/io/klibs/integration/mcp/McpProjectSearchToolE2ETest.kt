package io.klibs.integration.mcp

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.klibs.integration.mcp.dto.service.McpProjectSearchResultDto
import io.klibs.integration.mcp.mapper.McpToolMapper
import io.klibs.integration.mcp.service.DEFAULT_MAX_PACKAGES_PER_PROJECT
import io.klibs.integration.mcp.service.McpProjectSearchService
import io.klibs.integration.mcp.tool.LoggingToolCallback
import io.klibs.integration.mcp.tool.McpProjectSearchTool
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration
import org.springframework.ai.mcp.server.common.autoconfigure.StatelessToolCallbackConverterAutoConfiguration
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStatelessWebMvcAutoConfiguration
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

/**
 * Real end-to-end test: boots the Spring AI MCP server on a live HTTP port and invokes the
 * `searchProjects` tool through an actual MCP client. Omitting `maxPackagesPerProject` exercises
 * Spring AI's genuine parameter binding for an absent primitive tool param (KTL-4688), instead of
 * emulating it via reflection.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.ai.mcp.server.protocol=stateless",
        "spring.ai.mcp.server.name=klibs-mcp-server-test",
        "spring.ai.mcp.server.version=1.0.0",
    ]
)
class McpProjectSearchToolE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var mcpProjectSearchService: McpProjectSearchService

    private lateinit var client: McpSyncClient

    private val logger = LoggerFactory.getLogger(LoggingToolCallback::class.java) as Logger
    private val logAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        logAppender.start()
        logger.addAppender(logAppender)

        val transport = HttpClientStreamableHttpTransport.builder("http://localhost:$port")
            .endpoint("/mcp")
            .build()
        client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .build()
        client.initialize()
    }

    @AfterEach
    fun tearDown() {
        client.closeGracefully()
        logger.detachAppender(logAppender)
    }

    @Test
    fun `searchProjects tolerates an omitted maxPackagesPerProject over a real MCP call`() {
        whenever(mcpProjectSearchService.mcpProjectSearch(anyOrNull(), any(), any(), any()))
            .thenReturn(McpProjectSearchResultDto(projects = emptyList()))

        val result = client.callTool(CallToolRequest("searchProjects", mapOf("query" to "kotlin")))

        assertFalse(result.isError, "tool call returned an error: ${result.content()}")
        verify(mcpProjectSearchService).mcpProjectSearch(
            query = "kotlin",
            platforms = emptyList(),
            targetFilters = emptyMap(),
            maxPackagesPerProject = DEFAULT_MAX_PACKAGES_PER_PROJECT,
        )
    }

    @Test
    fun `logs tool failure at ERROR before surfacing it as an error result`() {
        whenever(mcpProjectSearchService.mcpProjectSearch(anyOrNull(), any(), any(), any()))
            .thenThrow(RuntimeException("boom"))

        val result = client.callTool(CallToolRequest("searchProjects", mapOf("query" to "kotlin")))

        assertTrue(result.isError, "expected tool call to surface an error result")
        val errors = logAppender.list.filter { it.level == Level.ERROR }
        assertEquals(1, errors.size, "expected exactly one ERROR log for the failed tool call")
        assertTrue(
            errors.single().formattedMessage.contains("searchProjects"),
            "ERROR log should name the failing tool: ${errors.single().formattedMessage}",
        )
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(
        // Minimal servlet + MCP stateless-webmvc stack; no DB/JPA/security/OpenAI autoconfig.
        TomcatServletWebServerAutoConfiguration::class,
        DispatcherServletAutoConfiguration::class,
        WebMvcAutoConfiguration::class,
        HttpMessageConvertersAutoConfiguration::class,
        JacksonAutoConfiguration::class,
        McpServerJsonMapperAutoConfiguration::class,
        McpServerStatelessAutoConfiguration::class,
        StatelessToolCallbackConverterAutoConfiguration::class,
        McpServerStatelessWebMvcAutoConfiguration::class,
    )
    class TestConfig {

        @Bean
        fun mcpToolMapper(): McpToolMapper = Mappers.getMapper(McpToolMapper::class.java)

        @Bean
        fun mcpProjectSearchTool(
            mcpProjectSearchService: McpProjectSearchService,
            mcpToolMapper: McpToolMapper,
        ): McpProjectSearchTool = McpProjectSearchTool(mcpProjectSearchService, mcpToolMapper)

        @Bean
        fun mcpToolCallbackProvider(mcpProjectSearchTool: McpProjectSearchTool): ToolCallbackProvider {
            val delegate = MethodToolCallbackProvider.builder()
                .toolObjects(mcpProjectSearchTool)
                .build()
            return ToolCallbackProvider {
                delegate.toolCallbacks.map { LoggingToolCallback(it) }.toTypedArray()
            }
        }
    }
}
