package io.klibs.core.project.repository

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ActiveProfiles("test")
class ProjectReadmeEmbeddingRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    @Sql("/sql/ProjectReadmeEmbeddingRepositoryTest/setup.sql")
    fun `findWithoutEmbedding returns a project that has a readme but no embedding`() {
        val project = assertNotNull(projectRepository.findWithoutEmbedding(), "expected a project without embedding")

        assertEquals(9301, project.id, "only the project with a readme and no embedding must be returned")
    }

    @Test
    @Sql("/sql/ProjectReadmeEmbeddingRepositoryTest/setup.sql")
    fun `updateReadmeEmbedding stores the embedding so the project is no longer pending`() {
        val embedding = FloatArray(1536) { 0.01f }

        projectRepository.updateReadmeEmbedding(9301, embedding)

        assertNull(projectRepository.findWithoutEmbedding(), "no project should remain without an embedding")
    }

    @Test
    @Sql("/sql/ProjectReadmeEmbeddingRepositoryTest/setup.sql")
    fun `updateMinimizedReadme invalidates the stored embedding`() {
        projectRepository.updateReadmeEmbedding(9301, FloatArray(1536) { 0.01f })
        assertNull(projectRepository.findWithoutEmbedding(), "precondition: embedding stored")

        projectRepository.updateMinimizedReadme(9301, "# Updated README content")

        val pending = assertNotNull(
            projectRepository.findWithoutEmbedding(),
            "changing the readme must drop the embedding so it gets recomputed"
        )
        assertEquals(9301, pending.id)
    }
}
