package io.klibs.integration.mcp.tool

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.metadata.ToolMetadata

/**
 * Wraps a [ToolCallback] to log failures at ERROR before rethrowing. The MCP server otherwise
 * swallows tool exceptions into an isError result without any log line. Covers argument-binding
 * failures too, since binding happens inside [ToolCallback.call].
 */
class LoggingToolCallback(private val delegate: ToolCallback) : ToolCallback {

    private val logger: Logger = LoggerFactory.getLogger(LoggingToolCallback::class.java)

    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun getToolMetadata(): ToolMetadata = delegate.toolMetadata

    override fun call(toolInput: String): String = logging { delegate.call(toolInput) }

    override fun call(toolInput: String, toolContext: ToolContext?): String =
        logging { delegate.call(toolInput, toolContext) }

    private inline fun logging(block: () -> String): String =
        try {
            block()
        } catch (e: Exception) {
            logger.error("MCP tool '{}' failed", delegate.toolDefinition.name(), e)
            throw e
        }
}
