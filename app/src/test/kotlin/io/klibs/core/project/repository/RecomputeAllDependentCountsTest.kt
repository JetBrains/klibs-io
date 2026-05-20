package io.klibs.core.project.repository

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals

@ActiveProfiles("test")
class RecomputeAllDependentCountsTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    @Sql("/sql/RecomputeAllDependentCountsTest/setup.sql")
    fun `recomputeAllDependentCounts walks maven_artifact and excludes self-dependencies`() {
        projectRepository.recomputeAllDependentCounts()

        val a = requireNotNull(projectRepository.findById(9101)) { "project A is missing" }
        val b = requireNotNull(projectRepository.findById(9102)) { "project B is missing" }
        val c = requireNotNull(projectRepository.findById(9103)) { "project C is missing" }

        assertEquals(1, a.dependentCount, "A should be referenced exactly by B")
        assertEquals(0, b.dependentCount, "B's only inbound edge is its own self-dependency — must be filtered out")
        assertEquals(0, c.dependentCount, "C has no inbound dependencies")
    }
}
