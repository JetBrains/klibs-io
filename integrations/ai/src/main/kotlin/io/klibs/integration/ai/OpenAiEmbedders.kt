package io.klibs.integration.ai

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Default embedder backed by OpenAI `text-embedding-3-small` (1536 dims), stored in the
 * original `readme_embedding` column. Delegates to [AiService.embed] to reuse its metrics.
 */
@Component
@ConditionalOnProperty("klibs.embeddings.ai-small.enabled", havingValue = "true")
class OpenAiSmallEmbedder(
    private val aiService: AiService,
) : Embedder {
    override val embedderName = "openai-3-small"
    override val dimensions = AiService.EMBEDDING_DIMENSIONS
    override val columnName = "readme_embedding"

    override fun embed(text: String): FloatArray = aiService.embed(text)
}

/**
 * Embedder backed by OpenAI `text-embedding-3-large` (3072 dims).
 */
@Component
@ConditionalOnProperty("klibs.embeddings.ai-large.enabled", havingValue = "true")
class OpenAiLargeEmbedder(
    private val aiService: AiService,
) : Embedder {
    override val embedderName = "openai-3-large"
    override val dimensions = AiService.EMBEDDING_DIMENSIONS_LARGE
    override val columnName = "readme_embedding_openai_large"

    override fun embed(text: String): FloatArray =
        aiService.embed(text, AiService.EMBEDDING_MODEL_LARGE, dimensions)
}

/**
 * Embedder backed by the legacy OpenAI `text-embedding-ada-002` (1536 dims). The model does
 * not support a custom dimension count, so none is requested.
 */
@Component
@ConditionalOnProperty("klibs.embeddings.ai-ada.enabled", havingValue = "true")
class OpenAiAdaEmbedder(
    private val aiService: AiService,
) : Embedder {
    override val embedderName = "openai-ada-002"
    override val dimensions = AiService.EMBEDDING_DIMENSIONS_ADA
    override val columnName = "readme_embedding_openai_ada"

    override fun embed(text: String): FloatArray =
        aiService.embed(text, AiService.EMBEDDING_MODEL_ADA, null)
}
