package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.entity.MavenArtifactEntity
import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.repository.MavenArtifactRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class MavenArtifactService(
    private val mavenArtifactRepository: MavenArtifactRepository,
) {

    private val log = org.slf4j.LoggerFactory.getLogger(MavenArtifactService::class.java)

    /**
     * Resolves the given [coordinates] to their `maven_artifact` rows, inserting any
     * coordinates that are not yet present.
     *
     * The returned map has exactly one entry per element of [coordinates]; the input
     * order is **not** preserved.
     */
    fun resolveOrCreateAll(coordinates: Set<MavenCoordinatesDTO>): Map<MavenCoordinatesDTO, MavenArtifactDTO> {
        if (coordinates.isEmpty()) return emptyMap()

        val resolved = HashMap<MavenCoordinatesDTO, MavenArtifactDTO>(coordinates.size)

        // 1. Bulk-fetch the rows that already exist using a single JPQL query.
        val keys = coordinates.map(::pack)
        mavenArtifactRepository.findAllByPackedKey(keys).forEach { entity ->
            resolved[entity.toCoordinates()] = MavenArtifactDTO.fromEntity(entity)
        }

        // 2. Insert the rest one row at a time; on a unique-index conflict, re-read.
        coordinates.asSequence()
            .filter { it !in resolved }
            .forEach { coords ->
                resolved[coords] = MavenArtifactDTO.fromEntity(insertOrLookup(coords))
            }

        return resolved
    }

    /**
     * Single-coordinate convenience wrapper around [resolveOrCreateAll].
     * Returns the existing row or inserts a fresh one.
     */
    fun resolveOrCreate(coords: MavenCoordinatesDTO): MavenArtifactDTO {
        val existing = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(
            coords.groupId, coords.artifactId, coords.version,
        )
        return MavenArtifactDTO.fromEntity(existing ?: insertOrLookup(coords))
    }

    /**
     * Inserts a `maven_artifact` row for the given [coords] if it does not exist yet, then
     * reads and returns the row.
     */
    private fun insertOrLookup(coords: MavenCoordinatesDTO): MavenArtifactEntity {
        val inserted = mavenArtifactRepository.saveIfAbsent(
            coords.groupId, coords.artifactId, coords.version,
        )
        if (inserted == 0.toLong()) {
            log.debug("Lost race on ${coords.groupId}:${coords.artifactId}:${coords.version}, re-reading it")
        }
        return requireNotNull(
            mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(
                coords.groupId, coords.artifactId, coords.version,
            )
        ) {
            "maven_artifact row is still missing after upsert for $coords"
        }
    }

    private companion object {
        private fun pack(coords: MavenCoordinatesDTO): String =
            "${coords.groupId}|${coords.artifactId}|${coords.version}"

        private fun MavenArtifactEntity.toCoordinates(): MavenCoordinatesDTO =
            MavenCoordinatesDTO(groupId = groupId, artifactId = artifactId, version = version)
    }
}
