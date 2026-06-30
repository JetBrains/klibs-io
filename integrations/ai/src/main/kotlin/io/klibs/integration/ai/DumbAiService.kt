package io.klibs.integration.ai

import org.springframework.ai.chat.prompt.Prompt
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("klibs.ai", havingValue = "true")
class DumbAiService() : AiService {
    override fun executeOpenAiRequest(
        prompt: Prompt,
        methodName: String,
        model: String,
    ): String {
        return ""
    }

    override fun embed(text: String): FloatArray {
        return FloatArray(0)
    }
}
