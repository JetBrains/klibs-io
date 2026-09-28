package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.SuspiciousPackageCandidateKey
import io.klibs.core.pckg.enums.CandidateStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SEED = "classpath:sql/SuspiciousForkCandidateQueryDbTest/seed-candidates.sql"

@ActiveProfiles("test")
class SuspiciousForkCandidateQueryDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repository: SuspiciousPackageCandidateRepository

    @Test
    @Sql(SEED)
    fun `returns only pending github-namespaced candidates whose owner differs from the repository owner`() {
        assertEquals(listOf("com-github", "qualifies", "sibling-dep"), qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `resolves the repository owner through the repository, not the project`() {
        val candidate = repository.findForkBanCandidates().single { it.artifactId == "qualifies" }

        assertEquals("io.github.suspect", candidate.groupId)
        assertEquals("suspect", candidate.suspectOwner)
        assertEquals("cashapp", candidate.repoOwner)
        assertEquals("zipline", candidate.repoName)
    }

    @Test
    @Sql(SEED)
    fun `skips a candidate outside the github namespaces`() {
        assertFalse("plain" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `compares owners case-insensitively`() {
        assertFalse("own-case" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `skips a candidate a reviewer has resolved`() {
        assertFalse("reviewed" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `skips a candidate depended on from another project or from no project`() {
        assertFalse("depended" in qualifyingArtifacts())
        assertFalse("orphan-dep" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `keeps a candidate depended on only within its own project`() {
        assertTrue("sibling-dep" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `skips a candidate already banned exactly or by group`() {
        assertFalse("banned-exact" in qualifyingArtifacts())
        assertFalse("banned-group" in qualifyingArtifacts())
    }

    @Test
    @Sql(SEED)
    fun `markResolved resolves a pending candidate with the given notes`() {
        val updated = repository.markResolved(48001, "qualifies", "io.github.suspect", "auto-banned")

        assertEquals(1, updated)
        val candidate = candidate("qualifies", "io.github.suspect")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("auto-banned", candidate.notes)
    }

    @Test
    @Sql(SEED)
    fun `markResolved leaves a reviewed candidate unchanged`() {
        val updated = repository.markResolved(48001, "reviewed", "io.github.suspect", "auto-banned")

        assertEquals(0, updated)
        val candidate = candidate("reviewed", "io.github.suspect")
        assertEquals(CandidateStatus.RESOLVED, candidate.status)
        assertEquals("kept by reviewer", candidate.notes)
    }

    private fun qualifyingArtifacts(): List<String> = repository.findForkBanCandidates().map { it.artifactId }

    private fun candidate(artifactId: String, groupId: String) =
        repository.findById(SuspiciousPackageCandidateKey(48001, artifactId, groupId)).get()
}
