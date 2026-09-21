package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.dto.MavenCoordinateDTO
import io.klibs.core.pckg.entity.MavenCoordinateEntity
import io.klibs.core.pckg.repository.MavenCoordinateRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class MavenCoordinateServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: MavenCoordinateService

    @MockitoSpyBean
    private lateinit var mavenCoordinateRepository: MavenCoordinateRepository

    @Test
    fun `resolveOrCreateAll returns empty set for empty input`() {
        val result = uut.resolveOrCreateAll(emptySet())

        assertTrue(result.isEmpty())
        assertEquals(0L, mavenCoordinateRepository.count())
    }

    @Test
    fun `resolveOrCreateAll inserts new coordinates and returns the resolved entities`() {
        val coords = setOf(
            MavenCoordinateDTO("io.klibs", "alpha", "1.0.0"),
            MavenCoordinateDTO("io.klibs", "alpha", "1.1.0"),
            MavenCoordinateDTO("io.klibs", "beta", "1.0.0"),
        )

        val resolved = uut.resolveOrCreateAll(coords)

        val ids = resolved.map { requireNotNull(it.id) }
        assertEquals(coords.size, ids.toSet().size)
        assertEquals(coords.size.toLong(), mavenCoordinateRepository.count())
        coords.forEach { c ->
            val stored = mavenCoordinateRepository.findByGroupIdAndArtifactIdAndVersion(
                c.groupId, c.artifactId, c.version,
            )
            assertNotNull(stored, "Expected $c to be persisted")
            val matching = resolved.first { it.groupId == c.groupId && it.artifactId == c.artifactId && it.version == c.version }
            assertEquals(stored.id, matching.id)
        }
    }

    @Test
    fun `resolveOrCreateAll is idempotent for duplicates and reuses existing rows`() {
        val preSavedEntity = mavenCoordinateRepository.save(
            MavenCoordinateEntity(groupId = "io.klibs", artifactId = "alpha", version = "1.0.0")
        )
        val preSavedEntityId = requireNotNull(preSavedEntity.id)

        val coords = setOf(
            MavenCoordinateDTO("io.klibs", "alpha", "1.0.0"),
            MavenCoordinateDTO("io.klibs", "alpha", "2.0.0"),
        )

        val firstRun = uut.resolveOrCreateAll(coords)
        val secondRun = uut.resolveOrCreateAll(coords)

        val alphaFirst = firstRun.first { it.artifactId == "alpha" && it.version == "1.0.0" }
        assertEquals(preSavedEntityId, alphaFirst.id)
        assertEquals(firstRun.map { it.id }.toSet(), secondRun.map { it.id }.toSet())
        assertEquals(2L, mavenCoordinateRepository.count())
    }

    @Test
    fun `resolveOrCreate recovers existing row when saveIfAbsent loses the race`() {
        val coords = MavenCoordinateDTO("io.klibs", "race", "1.0.0")
        val concurrentlyInserted = mavenCoordinateRepository.save(
            MavenCoordinateEntity(groupId = coords.groupId, artifactId = coords.artifactId, version = coords.version)
        )
        val expectedId = requireNotNull(concurrentlyInserted.id)

        doReturn(null, concurrentlyInserted)
            .whenever(mavenCoordinateRepository)
            .findByGroupIdAndArtifactIdAndVersion(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))

        val result = uut.resolveOrCreate(coords)

        assertEquals(expectedId, result.id)
        assertEquals(coords.groupId, result.groupId)
        assertEquals(coords.artifactId, result.artifactId)
        assertEquals(coords.version, result.version)
        verify(mavenCoordinateRepository).saveIfAbsent(coords.groupId, coords.artifactId, coords.version)
    }

    @Test
    fun `insertOrLookup throws when saveIfAbsent lost race but row cannot be re-read`() {
        val coords = MavenCoordinateDTO("io.klibs", "missing", "1.0.0")
        doReturn(0L)
            .whenever(mavenCoordinateRepository)
            .saveIfAbsent(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))
        doReturn(null)
            .whenever(mavenCoordinateRepository)
            .findByGroupIdAndArtifactIdAndVersion(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))

        val ex = assertThrows<IllegalArgumentException> { uut.resolveOrCreate(coords) }
        assertTrue(ex.message!!.contains("maven_coordinate row after insert"))
    }
}
