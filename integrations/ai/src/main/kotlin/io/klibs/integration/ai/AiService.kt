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

    fun embed(text: String): FloatArray

    /**
     * Embeds [text] with the given OpenAI embedding [model]. When [dimensions] is not null it is
     * requested explicitly; some models (e.g. ada-002) do not support this and require null.
     */
    fun embed(text: String, model: String, dimensions: Int?): FloatArray

    companion object {
        const val DEFAULT_GPT = "gpt-5-mini"
        const val WEBSEARCH_GPT = "gpt-4o-search-preview"
        const val EMBEDDING_MODEL = "text-embedding-3-small"
        const val EMBEDDING_DIMENSIONS = 1536
        const val EMBEDDING_MODEL_LARGE = "text-embedding-3-large"
        const val EMBEDDING_DIMENSIONS_LARGE = 3072
        const val EMBEDDING_MODEL_ADA = "text-embedding-ada-002"
        const val EMBEDDING_DIMENSIONS_ADA = 1536
    }
}
