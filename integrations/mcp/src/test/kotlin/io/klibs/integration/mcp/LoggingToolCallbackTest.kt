package io.klibs.integration.mcp

import io.klibs.integration.mcp.tool.LoggingToolCallback
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition

class LoggingToolCallbackTest {

    private val toolDefinition = ToolDefinition.builder()
        .name("searchProjects")
        .description("test")
        .inputSchema("{}")
        .build()

    @Test
    fun `delegates successful call and returns its result`() {
        val delegate = mockk<ToolCallback>()
        every { delegate.call("input") } returns "result"
        every { delegate.toolDefinition } returns toolDefinition

        val result = LoggingToolCallback(delegate).call("input")

        assertEquals("result", result)
    }

    @Test
    fun `rethrows exception raised by the delegate`() {
        val failure = NullPointerException("Cannot invoke Number.intValue()")
        val delegate = mockk<ToolCallback>()
        every { delegate.call("input") } throws failure
        every { delegate.toolDefinition } returns toolDefinition

        val thrown = assertThrows(NullPointerException::class.java) {
            LoggingToolCallback(delegate).call("input")
        }
        assertEquals(failure.message, thrown.message)
    }
}
