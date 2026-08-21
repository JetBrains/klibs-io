package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.SuspiciousPackagePairCandidateEntity
import io.klibs.core.pckg.enums.CandidateStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
class SuspiciousPackagePairCandidateRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repository: SuspiciousPackagePairCandidateRepository

    @Test
    @Sql("/sql/SuspiciousPackagePairCandidateRepositoryTest/seed-project.sql")
    fun `candidate row round-trips through the schema`() {
        val firstReleaseTs = Instant.parse("2024-01-01T00:00:00Z")
        val lastReleaseTs = Instant.parse("2024-06-01T00:00:00Z")
        val detectedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val saved = repository.save(
            SuspiciousPackagePairCandidateEntity(
                projectId = 9001,
                artifactId = "kotlinx-coroutines-core",
                groupId = "io.github.impostor",
                versionCount = 3,
                firstReleaseTs = firstReleaseTs,
                lastReleaseTs = lastReleaseTs,
                derivedOwnerLogin = "impostor",
                status = CandidateStatus.PENDING,
                notes = null,
                detectedAt = detectedAt,
            ),
        )

        val found = repository.findById(requireNotNull(saved.id)).orElseThrow()

        assertNotNull(found.id, "generated id should be populated")
        assertEquals(9001, found.projectId)
        assertEquals("kotlinx-coroutines-core", found.artifactId)
        assertEquals("io.github.impostor", found.groupId)
        assertEquals(3, found.versionCount)
        assertEquals(firstReleaseTs, found.firstReleaseTs)
        assertEquals(lastReleaseTs, found.lastReleaseTs)
        assertEquals("impostor", found.derivedOwnerLogin)
        assertEquals(CandidateStatus.PENDING, found.status)
        assertEquals(detectedAt, found.detectedAt)
    }
}
