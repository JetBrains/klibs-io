package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateEntity
import io.klibs.core.pckg.enums.CandidateStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SEED = "classpath:sql/SuspiciousPackageCandidateCollectionDbTest/seed-conflicts.sql"

@ActiveProfiles("test")
class SuspiciousPackageCandidateCollectionDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repository: SuspiciousPackageCandidateRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Sql(SEED)
    fun `records one PENDING row per conflicting entry`() {
        val inserted = repository.insertMissingCandidates()

        assertEquals(5, inserted, "two entries for project 47001 plus three for 47002")
        val all = candidates()
        assertEquals(5, all.size)
        assertTrue(all.all { it.status == CandidateStatus.PENDING })
        assertEquals(
            listOf("io.github.beta", "org.alpha"),
            candidatesOf(47001).map { it.groupId },
        )
    }

    @Test
    @Sql(SEED)
    fun `counts distinct groupIds rather than package rows`() {
        repository.insertMissingCandidates()

        // org.zeta:solo has three versions under one groupId - not a conflict.
        assertTrue(candidatesOf(47003).isEmpty(), "a single groupId is never a conflict, however many versions")
        // io.github.beta:lib has one version but shares its artifactId with another groupId.
        assertTrue(
            candidatesOf(47001).any { it.groupId == "io.github.beta" },
            "a conflicting entry counts even when published only once",
        )
    }

    @Test
    @Sql(SEED)
    fun `records every entry of a conflict spanning three groupIds`() {
        repository.insertMissingCandidates()

        assertEquals(
            listOf("org.delta", "org.epsilon", "org.gamma"),
            candidatesOf(47002).map { it.groupId },
        )
    }

    @Test
    @Sql(SEED)
    fun `skips packages that belong to no project`() {
        repository.insertMissingCandidates()

        assertTrue(candidates().none { it.artifactId == "stray" })
    }

    @Test
    @Sql(SEED)
    fun `leaves notes empty on insert`() {
        repository.insertMissingCandidates()

        assertTrue(candidates().all { it.notes == null })
    }

    @Test
    @Sql(SEED)
    fun `preserves a reviewer's status and notes across runs without duplicating`() {
        repository.insertMissingCandidates()
        review(47001, "lib", "org.alpha", CandidateStatus.RESOLVED, "impersonation, banned")

        val inserted = repository.insertMissingCandidates()

        assertEquals(0, inserted, "everything already conflicting is already recorded")
        assertEquals(5, candidates().size)
        val reviewed = candidatesOf(47001).single { it.groupId == "org.alpha" }
        assertEquals(CandidateStatus.RESOLVED, reviewed.status)
        assertEquals("impersonation, banned", reviewed.notes)
    }

    @Test
    @Sql(SEED)
    fun `lets entries of one conflict hold independent status and notes`() {
        repository.insertMissingCandidates()

        review(47002, "tool", "org.gamma", CandidateStatus.RESOLVED, "the original")
        review(47002, "tool", "org.delta", CandidateStatus.RESOLVED, "a fork, kept")

        val byGroup = candidatesOf(47002).associateBy { it.groupId }
        assertEquals(CandidateStatus.RESOLVED, byGroup.getValue("org.gamma").status)
        assertEquals("the original", byGroup.getValue("org.gamma").notes)
        assertEquals(CandidateStatus.RESOLVED, byGroup.getValue("org.delta").status)
        assertEquals("a fork, kept", byGroup.getValue("org.delta").notes)
        assertEquals(CandidateStatus.PENDING, byGroup.getValue("org.epsilon").status)
        assertNull(byGroup.getValue("org.epsilon").notes)
    }

    @Test
    @Sql(SEED)
    fun `adds a groupId that joins a reviewed conflict later, leaving its siblings untouched`() {
        repository.insertMissingCandidates()
        review(47001, "lib", "org.alpha", CandidateStatus.RESOLVED, "the original")
        review(47001, "lib", "io.github.beta", CandidateStatus.RESOLVED, "a fork, kept")

        publish(47104, 47001, "org.late", "lib")
        val inserted = repository.insertMissingCandidates()

        assertEquals(1, inserted, "only the groupId that was not already recorded")
        val byGroup = candidatesOf(47001).associateBy { it.groupId }
        assertEquals(3, byGroup.size)
        assertEquals(CandidateStatus.PENDING, byGroup.getValue("org.late").status)
        assertNull(byGroup.getValue("org.late").notes)
        assertEquals(CandidateStatus.RESOLVED, byGroup.getValue("org.alpha").status)
        assertEquals("the original", byGroup.getValue("org.alpha").notes)
        assertEquals(CandidateStatus.RESOLVED, byGroup.getValue("io.github.beta").status)
        assertEquals("a fork, kept", byGroup.getValue("io.github.beta").notes)
    }

    @Test
    @Sql(SEED)
    fun `keeps rows whose entries stop conflicting`() {
        repository.insertMissingCandidates()
        review(47001, "lib", "org.alpha", CandidateStatus.RESOLVED, "banned")

        // Removing one entry leaves its sibling alone under one groupId, so both stop conflicting.
        jdbcTemplate.update("DELETE FROM package WHERE project_id = 47001 AND group_id = 'io.github.beta'")
        val inserted = repository.insertMissingCandidates()

        assertEquals(0, inserted)
        val remaining = candidatesOf(47001)
        assertEquals(2, remaining.size, "neither row is deleted when its entry stops conflicting")
        assertEquals(CandidateStatus.RESOLVED, remaining.single { it.groupId == "org.alpha" }.status)
        assertNotNull(remaining.singleOrNull { it.groupId == "io.github.beta" })
    }

    private fun candidates(): List<SuspiciousPackageCandidateEntity> =
        repository.findAll().sortedWith(compareBy({ it.projectId }, { it.groupId }))

    private fun candidatesOf(projectId: Int) = candidates().filter { it.projectId == projectId }

    private fun review(
        projectId: Int,
        artifactId: String,
        groupId: String,
        status: CandidateStatus,
        notes: String,
    ) {
        jdbcTemplate.update(
            """
            UPDATE suspicious_package_candidate SET status = ?, notes = ?
            WHERE project_id = ? AND artifact_id = ? AND group_id = ?
            """.trimIndent(),
            status.name, notes, projectId, artifactId, groupId,
        )
    }

    private fun publish(id: Int, projectId: Int, groupId: String, artifactId: String) {
        jdbcTemplate.update(
            "INSERT INTO maven_coordinate (id, group_id, artifact_id, version) VALUES (?, ?, ?, '1.0.0')",
            id, groupId, artifactId,
        )
        jdbcTemplate.update(
            """
            INSERT INTO package (id, project_id, release_ts, created_at, group_id, artifact_id, version, build_tool,
                                 build_tool_version, kotlin_version, developers, licenses, scraper_type,
                                 maven_coordinate_id)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, '1.0.0', 'gradle', '8.0', '2.0.0', '[]'::jsonb,
                    '[]'::jsonb, 'SEARCH_MAVEN', ?)
            """.trimIndent(),
            id, projectId, groupId, artifactId, id,
        )
    }
}
