package io.klibs.integration.ai

import org.springframework.stereotype.Service

/**
 * Resolves [Embedder]s by their [Embedder.embedderName]. Collects every embedder bean
 * so new techniques become available for indexing and search just by adding a bean.
 */
@Service
class EmbedderRegistry(embedders: List<Embedder>) {

    private val byName: Map<String, Embedder> = embedders.associateBy { it.embedderName }

    /** All registered embedders, e.g. for populating every embedding column during indexing. */
    val all: List<Embedder> = embedders

    /** The embedder used when no name is supplied. */
    val default: Embedder = byName[DEFAULT_EMBEDDER_NAME]
        ?: embedders.firstOrNull()
        ?: error("No Embedder beans are registered")

    /**
     * Returns the embedder for [name], or the [default] when [name] is null or blank.
     * @throws IllegalArgumentException when [name] does not match any registered embedder.
     */
    fun resolve(name: String?): Embedder {
        if (name.isNullOrBlank()) return default
        return byName[name]
            ?: throw IllegalArgumentException("Unknown embedder '$name'. Available: ${byName.keys}")
    }

    companion object {
        const val DEFAULT_EMBEDDER_NAME = "openai-3-small"
    }
}
