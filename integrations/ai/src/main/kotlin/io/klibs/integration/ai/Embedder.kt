package io.klibs.integration.ai

/**
 * A single embedding technique. Every embedder produces a vector for a text and is
 * backed by its own pgvector column on the `project` table, so different techniques
 * can be stored and compared side by side.
 */
interface Embedder {
    /** Unique name used to select this embedder at index and search time. */
    val embedderName: String

    /** Number of dimensions of the produced vector; must match the DB column definition. */
    val dimensions: Int

    /** Name of the pgvector column on the `project` table that stores this embedding. */
    val columnName: String

    /** Produces an embedding vector for the given text. */
    fun embed(text: String): FloatArray
}
