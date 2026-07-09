package io.klibs.integration.mcp

import io.klibs.integration.mcp.tool.LoggingToolCallback
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
        val delegate = mock<ToolCallback> {
            on { toolDefinition } doReturn toolDefinition
            on { call("input") } doReturn "result"
        }

        val result = LoggingToolCallback(delegate).call("input")

        assertEquals("result", result)
    }

    @Test
    fun `rethrows exception raised by the delegate`() {
        val failure = NullPointerException("Cannot invoke Number.intValue()")
        val delegate = mock<ToolCallback> {
            on { toolDefinition } doReturn toolDefinition
        }
        whenever(delegate.call("input")).doThrow(failure)

        val thrown = assertThrows(NullPointerException::class.java) {
            LoggingToolCallback(delegate).call("input")
        }
        assertEquals(failure.message, thrown.message)
    }
}
