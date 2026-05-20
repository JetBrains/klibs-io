package io.klibs.core.pckg.service

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.entity.MavenArtifactEntity
import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.repository.MavenArtifactRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class MavenArtifactServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mavenArtifactService: MavenArtifactService

    @Autowired
    private lateinit var mavenArtifactRepository: MavenArtifactRepository

    @Test
    fun `resolveOrCreateAll returns empty map for empty input`() {
        val result = mavenArtifactService.resolveOrCreateAll(emptySet())

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

        val resolved = mavenArtifactService.resolveOrCreateAll(coords)

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

        val firstRun = mavenArtifactService.resolveOrCreateAll(coords)
        val secondRun = mavenArtifactService.resolveOrCreateAll(coords)

        assertEquals(preSavedEntityId, firstRun.getValue(MavenCoordinatesDTO("io.klibs", "alpha", "1.0.0")).id)
        assertEquals(firstRun.mapValues { it.value.id }, secondRun.mapValues { it.value.id },)
        assertEquals(2L, mavenArtifactRepository.count())
    }
}
