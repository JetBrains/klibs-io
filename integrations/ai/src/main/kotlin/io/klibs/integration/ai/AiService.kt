package io.klibs.integration.ai

import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
interface AiService {
    fun executeOpenAiRequest(
        prompt: Prompt,
        methodName: String,
        model: String,
    ): String

    fun executeWebSearchRequest(
        model: String,
        instructions: String,
        userContent: String,
        reasoningEffort: String,
        methodName: String,
    ): String

    companion object {
        const val DEFAULT_GPT = "gpt-5-mini"
        const val WEBSEARCH_GPT = "gpt-5-mini"
        const val WEBSEARCH_EFFORT = "low"
    }
}
