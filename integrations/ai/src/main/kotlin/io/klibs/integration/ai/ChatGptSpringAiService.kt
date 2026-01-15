package io.klibs.integration.ai

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
@Primary
@ConditionalOnProperty("klibs.ai", havingValue = "true")
class ChatGptSpringAiService(
    private val meterRegistry: MeterRegistry,
    private val chatModel: OpenAiChatModel
) : AiService {

    // Metrics for token usage
    private val promptTokensCounter = meterRegistry.counter("klibs.openai.tokens.prompt")
    private val completionTokensCounter = meterRegistry.counter("klibs.openai.tokens.completion")
    private val totalTokensCounter = meterRegistry.counter("klibs.openai.tokens.total")

    // Metric for rate limit
    private val rateLimitRequestsRemaining = AtomicLong(0)
    private val rateLimitTokensRemaining = AtomicLong(0)

    init {
        meterRegistry.gauge("klibs.openai.rate.limit.remaining.requests", rateLimitRequestsRemaining) { it.get().toDouble() }
        meterRegistry.gauge("klibs.openai.rate.limit.remaining.tokens", rateLimitTokensRemaining) { it.get().toDouble() }
    }

    /**
     * Helper method to execute OpenAI requests with timing and metrics recording
     *
     * @param prompt The prompt to send to OpenAI
     * @param methodName The name of the calling method for metrics tagging
     * @param model The model name for metrics tagging
     * @return The processed response content
     */
    override fun executeOpenAiRequest(
        prompt: Prompt,
        methodName: String,
        model: String,
    ): String {
        // Start timing the request
        val sample = Timer.start(meterRegistry)

        val response = try {
            chatModel.call(prompt)
        } finally {
            // Stop the timer immediately after the call, even if it throws
            sample.stop(meterRegistry.timer("klibs.openai.request.time",
                "method", methodName,
                "model", model))
        }

        // Record metrics from the response
        recordMetrics(response)

        // Process and return the response content
        return response.result.output.text ?: ""
    }

    private fun recordMetrics(response: ChatResponse) {
        response.metadata.apply {
            usage?.apply {
                promptTokens?.apply { promptTokensCounter.increment(this.toDouble()) }
                completionTokens?.apply { completionTokensCounter.increment(this.toDouble()) }
                totalTokens?.apply { totalTokensCounter.increment(this.toDouble()) }
            }
            rateLimit?.apply {
                requestsRemaining?.apply { rateLimitRequestsRemaining.set(this) }
                tokensRemaining?.apply { rateLimitTokensRemaining.set(this) }
            }
        }
    }
}
