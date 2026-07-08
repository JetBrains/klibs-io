package io.klibs.integration.ai

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.math.sqrt

/**
 * Strong, fully-offline embedder backed by a local sentence-transformers model
 * (default `BAAI/bge-large-en-v1.5`, 1024 dims) run through DJL. Meant as a real local
 * alternative to the OpenAI embedders for the search research: after a one-time model
 * download it needs no API key, no network and has no per-query cost.
 *
 * Disabled unless `klibs.embeddings.bge-large.enabled=true`, since the model (~0.8 GB) is
 * heavy and only relevant for the embedding comparison. The model is loaded lazily on first
 * use, so enabling the bean does not download anything at startup.
 */
@Component
@ConditionalOnProperty("klibs.embeddings.bge-large.enabled", havingValue = "true")
class BgeLargeLocalEmbedder(
    @Value("\${klibs.embeddings.bge-large.model-url:djl://ai.djl.huggingface.pytorch/BAAI/bge-large-en-v1.5}")
    private val modelUrl: String,
) : Embedder, DisposableBean {

    override val embedderName = "bge-large-local"
    override val dimensions = DIMENSIONS
    override val columnName = "readme_embedding_bge_large"

    private val model: Lazy<ZooModel<String, FloatArray>> = lazy { loadModel() }
    private val predictor: Lazy<Predictor<String, FloatArray>> = lazy { model.value.newPredictor() }

    override fun embed(text: String): FloatArray {
        // DJL predictors are not thread-safe; the indexing job and search may call concurrently.
        val raw = synchronized(this) { predictor.value.predict(text) }
        return normalize(raw)
    }

    private fun loadModel(): ZooModel<String, FloatArray> {
        logger.info("Loading local embedding model from {} (first run downloads ~0.8 GB)", modelUrl)
        val criteria = Criteria.builder()
            .setTypes(String::class.java, FloatArray::class.java)
            .optModelUrls(modelUrl)
            .optEngine("PyTorch")
            .optTranslatorFactory(TextEmbeddingTranslatorFactory())
            .build()
        return criteria.loadModel()
    }

    override fun destroy() {
        if (predictor.isInitialized()) predictor.value.close()
        if (model.isInitialized()) model.value.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BgeLargeLocalEmbedder::class.java)
        private const val DIMENSIONS = 1024

        /** L2-normalizes the vector in place so cosine distance is comparable across embedders. */
        internal fun normalize(vector: FloatArray): FloatArray {
            val norm = sqrt(vector.fold(0.0) { acc, v -> acc + v * v }).toFloat()
            if (norm > 0f) {
                for (i in vector.indices) vector[i] /= norm
            }
            return vector
        }
    }
}
