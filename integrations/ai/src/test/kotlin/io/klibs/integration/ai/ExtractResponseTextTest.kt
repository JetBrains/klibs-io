package io.klibs.integration.ai

import com.openai.core.JsonValue
import com.openai.models.responses.ResponseOutputItem
import com.openai.models.responses.ResponseOutputMessage
import com.openai.models.responses.ResponseOutputText
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExtractResponseTextTest {

    @Test
    fun `joins output text parts within a message`() {
        val result = extractResponseText(listOf(message("Hello ", "world")))
        assertEquals("Hello world", result)
    }

    @Test
    fun `concatenates text across multiple messages`() {
        val result = extractResponseText(listOf(message("A"), message("B")))
        assertEquals("AB", result)
    }

    @Test
    fun `returns empty string when there are no message items`() {
        assertEquals("", extractResponseText(emptyList()))
    }

    private fun outputText(text: String): ResponseOutputText =
        ResponseOutputText.builder()
            .text(text)
            .annotations(emptyList())
            .build()

    private fun message(vararg parts: String): ResponseOutputItem =
        ResponseOutputItem.ofMessage(
            ResponseOutputMessage.builder()
                .id("msg-1")
                .role(JsonValue.from("assistant"))
                .status(ResponseOutputMessage.Status.COMPLETED)
                .apply { parts.forEach { addContent(outputText(it)) } }
                .build()
        )
}
