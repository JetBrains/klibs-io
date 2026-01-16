package io.klibs.integration.ai

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.metadata.OpenAiRateLimit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Test class for verifying that ChatGptSpringAiService correctly collects OpenAI metrics.
 * This test directly increments counters to simulate what would happen when metrics are collected.
 */
class ChatGptSpringAiServiceOpenAiMetricsTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var chatModel: OpenAiChatModel
    private lateinit var chatGptSpringAiService: ChatGptSpringAiService

    @BeforeEach
    fun setup() {
        // Create a simple meter registry for testing
        meterRegistry = SimpleMeterRegistry()

        // Mock the OpenAI chat model
        chatModel = mockk()

        // Create the service with mocked dependencies
        chatGptSpringAiService = ChatGptSpringAiService(
            meterRegistry = meterRegistry,
            chatModel = chatModel
        )
    }

    @Test
    fun `should record token usage, rate limit metrics and timer when making OpenAI requests`() {
        val testUsage = mockk<Usage>()
        every { testUsage.getPromptTokens() } returns 100
        every { testUsage.getCompletionTokens() } returns 50
        every { testUsage.getTotalTokens() } returns 150

        // Create a simple mock response
        val mockResponse = mockk<ChatResponse>()

        val metadata = ChatResponseMetadata.builder()
            .usage(testUsage)
            .rateLimit(
                OpenAiRateLimit(
                    1,
                    2,
                    3.seconds.toJavaDuration(),
                    4,
                    5,
                    6.seconds.toJavaDuration()
                )
            )
            .build()

        // We'll set up the mock after creating the prompt below
        every { mockResponse.metadata } returns metadata

        // Act - Call the service method
        try {
            val model = "gpt-4o"
            val systemMessage = "You are a helpful assistant"
            val userMessage = "Tell me about Kotlin"
            val temperature = 0.7

            val messages = listOf(
                SystemMessage(systemMessage),
                UserMessage(userMessage)
            )

            val options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build()

            val prompt = Prompt(messages, options)

            // Set up the mock to return our response for the specific prompt instance
            every { chatModel.call(prompt) } returns mockResponse

            chatGptSpringAiService.executeOpenAiRequest(prompt, "ask", model)
        } catch (e: Exception) {
            // We expect an exception because our mock is incomplete
            // but the timer metrics should still be recorded
        }

        // Assert - Verify token metrics were collected
        val promptTokensCounter = meterRegistry.find("klibs.openai.tokens.prompt").counter()
            ?: error("Prompt tokens counter should be registered")
        val completionTokensCounter = meterRegistry.find("klibs.openai.tokens.completion").counter()
            ?: error("Completion tokens counter should be registered")
        val totalTokensCounter = meterRegistry.find("klibs.openai.tokens.total").counter()
            ?: error("Total tokens counter should be registered")

        assertEquals(100.0, promptTokensCounter.count(), "Prompt tokens count should match")
        assertEquals(50.0, completionTokensCounter.count(), "Completion tokens count should match")
        assertEquals(150.0, totalTokensCounter.count(), "Total tokens count should match")

        // Assert - Verify timer metrics were collected
        val timer = meterRegistry.find("klibs.openai.request.time")
            .tag("method", "ask")
            .tag("model", "gpt-4o")
            .timer()

        assertNotNull(timer, "Request timer should be registered")
        assertEquals(1, timer.count(), "Timer should have recorded one request")

        // Assert - Verify rate limit gauge metrics were updated
        val requestsRemainingGauge = meterRegistry.find("klibs.openai.rate.limit.remaining.requests").gauge()
            ?: error("Rate limit requests gauge should be registered")
        val tokensRemainingGauge = meterRegistry.find("klibs.openai.rate.limit.remaining.tokens").gauge()
            ?: error("Rate limit tokens gauge should be registered")

        assertEquals(2.0, requestsRemainingGauge.value(), "Rate limit requests remaining should be updated")
        assertEquals(5.0, tokensRemainingGauge.value(), "Rate limit tokens remaining should be updated")
    }
}