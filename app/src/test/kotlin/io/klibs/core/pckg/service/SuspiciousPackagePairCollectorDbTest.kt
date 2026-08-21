package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.SuspiciousPackagePairCandidateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class SuspiciousPackagePairCollectorDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var collector: SuspiciousPackagePairCollector

    @Autowired
    private lateinit var repository: SuspiciousPackagePairCandidateRepository

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Test
    @Sql("/sql/SuspiciousPackagePairCollectorDbTest/seed-conflicts.sql")
    fun `recompute records only cross-group conflicts as PENDING with correct aggregates`() {
        collector.recompute()

        val rows = repository.findAll().toList()
        // Only conflict-lib qualifies: solo-lib (single group) and orphan (project_id NULL) are excluded.
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.artifactId == "conflict-lib" })
        assertTrue(rows.all { it.status == CandidateStatus.PENDING })

        val alice = rows.first { it.groupId == "io.github.alice" }
        assertEquals("alice", alice.derivedOwnerLogin)
        assertEquals(2, alice.versionCount)
        assertTrue(alice.firstReleaseTs < alice.lastReleaseTs)

        val other = rows.first { it.groupId == "com.example" }
        assertNull(other.derivedOwnerLogin)
        assertEquals(1, other.versionCount)
        assertEquals(other.firstReleaseTs, other.lastReleaseTs)
    }

    @Test
    @Sql("/sql/SuspiciousPackagePairCollectorDbTest/seed-conflicts.sql")
    fun `re-run preserves reviewer status and notes without duplicating rows`() {
        collector.recompute()

        val alice = repository.findAll().first { it.groupId == "io.github.alice" }
        repository.save(alice.copy(status = CandidateStatus.RESOLVED, notes = "manually reviewed"))

        collector.recompute()

        val rows = repository.findAll().toList()
        assertEquals(2, rows.size)
        val reviewed = rows.first { it.groupId == "io.github.alice" }
        assertEquals(CandidateStatus.RESOLVED, reviewed.status)
        assertEquals("manually reviewed", reviewed.notes)
    }

    @Test
    @Sql("/sql/SuspiciousPackagePairCollectorDbTest/seed-conflicts.sql")
    fun `candidate rows are kept after their package rows are removed`() {
        collector.recompute()
        assertEquals(2, repository.count())

        packageRepository.deleteAll()
        collector.recompute()

        assertEquals(2, repository.count())
    }
}
