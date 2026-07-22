package io.klibs.integration.ai

import com.openai.client.OpenAIClient
import com.openai.models.Reasoning
import com.openai.models.ReasoningEffort
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseOutputItem
import com.openai.models.responses.WebSearchTool
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
    private val chatModel: OpenAiChatModel,
    private val openAiClient: OpenAIClient
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
        val response = timed(methodName, model) { chatModel.call(prompt) }

        response.metadata.apply {
            recordMetrics(
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens,
                requestsRemaining = rateLimit.requestsRemaining,
                tokensRemaining = rateLimit.tokensRemaining,
            )
        }

        return response.result?.output?.text ?: ""
    }

    override fun executeWebSearchRequest(
        model: String,
        instructions: String,
        userContent: String,
        reasoningEffort: String,
        methodName: String,
    ): String {
        val params = ResponseCreateParams.builder()
            .model(model)
            .instructions(instructions)
            .input(userContent)
            .addTool(WebSearchTool.builder().type(WebSearchTool.Type.WEB_SEARCH).build())
            .reasoning(Reasoning.builder().effort(ReasoningEffort.of(reasoningEffort)).build())
            .build()

        val httpResponse = timed(methodName, model) {
            openAiClient.responses().withRawResponse().create(params)
        }

        val response = httpResponse.parse()
        val usage = response.usage().orElse(null)
        val headers = httpResponse.headers()
        recordMetrics(
            promptTokens = usage?.inputTokens(),
            completionTokens = usage?.outputTokens(),
            totalTokens = usage?.totalTokens(),
            requestsRemaining = headers.values("x-ratelimit-remaining-requests").firstOrNull()?.toLongOrNull(),
            tokensRemaining = headers.values("x-ratelimit-remaining-tokens").firstOrNull()?.toLongOrNull(),
        )

        return extractResponseText(response.output())
    }

    private fun <T> timed(methodName: String, model: String, request: () -> T): T {
        val sample = Timer.start(meterRegistry)
        return try {
            request()
        } finally {
            sample.stop(meterRegistry.timer("klibs.openai.request.time", "method", methodName, "model", model))
        }
    }

    private fun recordMetrics(
        promptTokens: Number?,
        completionTokens: Number?,
        totalTokens: Number?,
        requestsRemaining: Number?,
        tokensRemaining: Number?,
    ) {
        promptTokens?.let { promptTokensCounter.increment(it.toDouble()) }
        completionTokens?.let { completionTokensCounter.increment(it.toDouble()) }
        totalTokens?.let { totalTokensCounter.increment(it.toDouble()) }
        requestsRemaining?.let { rateLimitRequestsRemaining.set(it.toLong()) }
        tokensRemaining?.let { rateLimitTokensRemaining.set(it.toLong()) }
    }
}

internal fun extractResponseText(output: List<ResponseOutputItem>): String =
    output.filter { it.isMessage() }
        .flatMap { it.asMessage().content() }
        .filter { it.isOutputText() }
        .joinToString("") { it.asOutputText().text() }
