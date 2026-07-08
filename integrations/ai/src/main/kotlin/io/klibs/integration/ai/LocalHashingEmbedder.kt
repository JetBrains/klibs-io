package io.klibs.integration.ai

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.math.sqrt

/**
 * Dependency-free, deterministic embedder used as an offline baseline for comparison.
 * Builds an L2-normalized bag-of-words vector by hashing lowercased word tokens into
 * [DIMENSIONS] buckets, so cosine distance reflects lexical overlap.
 */
@Component
@ConditionalOnProperty("klibs.embeddings.local-hashing.enabled", havingValue = "true")
class LocalHashingEmbedder : Embedder {
    override val embedderName = "local"
    override val dimensions = DIMENSIONS
    override val columnName = "readme_embedding_local"

    override fun embed(text: String): FloatArray {
        val vector = FloatArray(DIMENSIONS)
        TOKEN_REGEX.findAll(text.lowercase()).forEach { match ->
            val bucket = Math.floorMod(match.value.hashCode(), DIMENSIONS)
            vector[bucket] += 1.0f
        }

        val norm = sqrt(vector.fold(0.0) { acc, v -> acc + v * v }).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }

    private companion object {
        const val DIMENSIONS = 256
        val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
    }
}
