package io.klibs.integration.ai

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class ProjectReadmeEmbeddingGenerator(
    private val aiService: AiService
) {
    fun generateReadmeEmbedding(readmeMdContent: String): FloatArray {
        return aiService.embed(readmeMdContent)
    }
}
