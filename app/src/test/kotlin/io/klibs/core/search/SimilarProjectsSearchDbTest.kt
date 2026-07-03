package io.klibs.core.search

import BaseUnitWithDbLayerTest
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.search.service.SearchService
import io.klibs.integration.ai.EmbedderRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals

@ActiveProfiles("test")
class SimilarProjectsSearchDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var embedderRegistry: EmbedderRegistry

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql("/sql/SimilarProjectsSearchDbTest/seed.sql")
    fun `searchSimilarProjects returns projects ordered by embedding similarity, skipping those without an embedding`() {
        // project 60001 points along dim 0, 60002 along dim 1, 60003 along dim 2; 60004 has no embedding.
        projectRepository.updateReadmeEmbedding(60001, unitVector(0))
        projectRepository.updateReadmeEmbedding(60002, unitVector(1))
        projectRepository.updateReadmeEmbedding(60003, unitVector(2))

        // Query closest to dim 0, then dim 1; far from dim 2.
        val queryEmbedding = FloatArray(EMBEDDING_DIMENSIONS).apply {
            this[0] = 0.8f
            this[1] = 0.6f
        }
        whenever(aiService.embed(any())).thenReturn(queryEmbedding)

        val results = searchService.searchSimilarProjects(query = "anything", page = 1, limit = 20)

        assertEquals(
            listOf(60001, 60002, 60003),
            results.map { it.id },
            "Projects must be ordered by similarity; the project without an embedding must be excluded"
        )
    }

    @Test
    @Sql("/sql/SimilarProjectsSearchDbTest/seed.sql")
    fun `searchSimilarProjects uses the embedder selected by name and its own embedding column`() {
        val local = embedderRegistry.resolve("local")
        // Store local embeddings computed from distinct texts.
        projectRepository.updateReadmeEmbedding(60001, local.columnName, local.embed("kotlin coroutines flow channels"))
        projectRepository.updateReadmeEmbedding(60002, local.columnName, local.embed("android jetpack compose ui"))
        projectRepository.updateReadmeEmbedding(60003, local.columnName, local.embed("postgres database sql migration"))

        val results = searchService.searchSimilarProjects(
            query = "kotlin coroutines flow channels",
            embedderName = "local",
            page = 1,
            limit = 20
        )

        assertEquals(
            60001,
            results.first().id,
            "The project whose text matches the query must rank first for the selected embedder"
        )
    }

    private fun unitVector(index: Int): FloatArray =
        FloatArray(EMBEDDING_DIMENSIONS).apply { this[index] = 1.0f }

    private companion object {
        private const val EMBEDDING_DIMENSIONS = 1536
    }
}
