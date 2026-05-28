package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.MavenArtifactEntity
import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.repository.MavenArtifactRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class MavenArtifactServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: MavenArtifactService

    @MockitoSpyBean
    private lateinit var mavenArtifactRepository: MavenArtifactRepository

    @Test
    fun `resolveOrCreateAll returns empty map for empty input`() {
        val result = uut.resolveOrCreateAll(emptySet())

        assertTrue(result.isEmpty())
        assertEquals(0L, mavenArtifactRepository.count())
    }

    @Test
    fun `resolveOrCreateAll inserts new coordinates and returns the resolved entities`() {
        val coords = setOf(
            MavenCoordinatesDTO("io.klibs", "alpha", "1.0.0"),
            MavenCoordinatesDTO("io.klibs", "alpha", "1.1.0"),
            MavenCoordinatesDTO("io.klibs", "beta", "1.0.0"),
        )

        val resolved = uut.resolveOrCreateAll(coords)

        assertEquals(coords, resolved.keys)
        val ids = resolved.values.map { requireNotNull(it.id) }
        assertEquals(coords.size, ids.toSet().size)
        assertEquals(coords.size.toLong(), mavenArtifactRepository.count())
        coords.forEach { c ->
            val stored = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(
                c.groupId, c.artifactId, c.version,
            )
            assertNotNull(stored, "Expected $c to be persisted")
            assertEquals(resolved.getValue(c).id, stored.id)
        }
    }

    @Test
    fun `resolveOrCreateAll is idempotent for duplicates and reuses existing rows`() {
        val preSavedEntity = mavenArtifactRepository.save(
            MavenArtifactEntity(groupId = "io.klibs", artifactId = "alpha", version = "1.0.0")
        )
        val preSavedEntityId = requireNotNull(preSavedEntity.id)

        val coords = setOf(
            MavenCoordinatesDTO("io.klibs", "alpha", "1.0.0"),
            MavenCoordinatesDTO("io.klibs", "alpha", "1.0.0"),
            MavenCoordinatesDTO("io.klibs", "alpha", "2.0.0"),
        )

        val firstRun = uut.resolveOrCreateAll(coords)
        val secondRun = uut.resolveOrCreateAll(coords)

        assertEquals(preSavedEntityId, firstRun.getValue(MavenCoordinatesDTO("io.klibs", "alpha", "1.0.0")).id)
        assertEquals(firstRun.mapValues { it.value.id }, secondRun.mapValues { it.value.id })
        assertEquals(2L, mavenArtifactRepository.count())
    }

    @Test
    fun `resolveOrCreate recovers existing row when saveIfAbsent loses the race`() {
        val coords = MavenCoordinatesDTO("io.klibs", "race", "1.0.0")
        val concurrentlyInserted = mavenArtifactRepository.save(
            MavenArtifactEntity(groupId = coords.groupId, artifactId = coords.artifactId, version = coords.version)
        )
        val expectedId = requireNotNull(concurrentlyInserted.id)

        doReturn(null, concurrentlyInserted)
            .whenever(mavenArtifactRepository)
            .findByGroupIdAndArtifactIdAndVersion(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))

        val result = uut.resolveOrCreate(coords)

        assertEquals(expectedId, result.id)
        assertEquals(coords.groupId, result.groupId)
        assertEquals(coords.artifactId, result.artifactId)
        assertEquals(coords.version, result.version)
        verify(mavenArtifactRepository).saveIfAbsent(coords.groupId, coords.artifactId, coords.version)
    }

    @Test
    fun `insertOrLookup throws when saveIfAbsent lost race but row cannot be re-read`() {
        val coords = MavenCoordinatesDTO("io.klibs", "missing", "1.0.0")
        doReturn(0L)
            .whenever(mavenArtifactRepository)
            .saveIfAbsent(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))
        doReturn(null)
            .whenever(mavenArtifactRepository)
            .findByGroupIdAndArtifactIdAndVersion(eq(coords.groupId), eq(coords.artifactId), eq(coords.version))

        val ex = assertThrows<IllegalArgumentException> { uut.resolveOrCreate(coords) }
        assertTrue(ex.message!!.contains("maven_artifact row is still missing after upsert"))
    }
}
