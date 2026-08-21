package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.projection.SuspiciousPackagePair
import io.klibs.core.pckg.entity.SuspiciousPackagePairCandidateEntity
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.service.SuspiciousPackagePairCollector.Companion.deriveOwnerLogin
import io.klibs.core.pckg.service.SuspiciousPackagePairCollector.Companion.merge
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SuspiciousPackagePairCollectorUnitTest {

    @Test
    fun `deriveOwnerLogin maps github hosts to a lower-cased owner`() {
        assertEquals("alice", deriveOwnerLogin("io.github.alice"))
        assertEquals("bob", deriveOwnerLogin("com.github.bob"))
        assertEquals("alice", deriveOwnerLogin("io.github.Alice.sublib"))
        assertEquals("owner", deriveOwnerLogin("com.github.OWNER"))
    }

    @Test
    fun `deriveOwnerLogin returns null for non-github hosts`() {
        assertNull(deriveOwnerLogin("org.example"))
        assertNull(deriveOwnerLogin("io.gitlab.alice"))
        assertNull(deriveOwnerLogin("com.example.github"))
        assertNull(deriveOwnerLogin("io.github."))
    }

    @Test
    fun `merge builds a fresh PENDING row for a new conflict`() {
        val detectedAt = Instant.parse("2026-08-21T00:00:00Z")
        val view = pair(groupId = "io.github.alice", versionCount = 2)

        val row = merge(existing = null, view = view, derivedOwnerLogin = "alice", detectedAt = detectedAt)

        assertNull(row.id)
        assertEquals(CandidateStatus.PENDING, row.status)
        assertNull(row.notes)
        assertEquals("alice", row.derivedOwnerLogin)
        assertEquals(detectedAt, row.detectedAt)
        assertEquals(2, row.versionCount)
    }

    @Test
    fun `merge preserves reviewer status, notes, id and detected_at while refreshing signals`() {
        val originalDetectedAt = Instant.parse("2026-01-01T00:00:00Z")
        val existing = SuspiciousPackagePairCandidateEntity(
            id = 42L,
            projectId = 7,
            artifactId = "lib",
            groupId = "io.github.alice",
            versionCount = 1,
            firstReleaseTs = Instant.parse("2025-01-01T00:00:00Z"),
            lastReleaseTs = Instant.parse("2025-02-01T00:00:00Z"),
            derivedOwnerLogin = "alice",
            status = CandidateStatus.RESOLVED,
            notes = "checked, safe",
            detectedAt = originalDetectedAt,
        )
        val view = pair(
            groupId = "io.github.alice",
            versionCount = 5,
            lastReleaseTs = Instant.parse("2026-08-01T00:00:00Z"),
        )

        val row = merge(existing, view, derivedOwnerLogin = "alice", detectedAt = Instant.parse("2026-08-21T00:00:00Z"))

        assertEquals(42L, row.id)
        assertEquals(CandidateStatus.RESOLVED, row.status)
        assertEquals("checked, safe", row.notes)
        assertEquals(originalDetectedAt, row.detectedAt)
        assertEquals(5, row.versionCount)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), row.lastReleaseTs)
    }

    private fun pair(
        projectId: Int = 7,
        artifactId: String = "lib",
        groupId: String,
        versionCount: Int,
        firstReleaseTs: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        lastReleaseTs: Instant = Instant.parse("2025-02-01T00:00:00Z"),
    ) = SuspiciousPackagePair(projectId, artifactId, groupId, versionCount, firstReleaseTs, lastReleaseTs)
}
