package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.MavenCoordinateDTO
import io.klibs.core.pckg.entity.MavenCoordinateEntity
import io.klibs.core.pckg.repository.MavenCoordinateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class MavenCoordinateService(
    private val mavenCoordinateRepository: MavenCoordinateRepository,
) {

    private val log = org.slf4j.LoggerFactory.getLogger(MavenCoordinateService::class.java)

    /**
     * Resolves all given coordinates to their persisted [MavenCoordinateDTO]s.
     * Missing rows are inserted with `ON CONFLICT DO NOTHING` and then re-read.
     *
     * The input is deduplicated by the [Set] semantics; return
     * order is **not** preserved.
     */
    fun resolveOrCreateAll(coordinates: Set<MavenCoordinateDTO>): Set<MavenCoordinateDTO> {
        if (coordinates.isEmpty()) return emptySet()

        val resolved = HashSet<MavenCoordinateDTO>(coordinates.size)

        // 1. Bulk-fetch the rows that already exist using a single JPQL query.
        val keys = coordinates.map(::pack)
        val existing = mavenCoordinateRepository.findAllByPackedKey(keys)
        existing.mapTo(resolved, MavenCoordinateDTO::fromEntity)

        // 2. Insert any coordinates that weren't found, then re-query them.
        if (resolved.size < coordinates.size) {
            val foundKeys = existing.mapTo(HashSet(existing.size), ::pack)
            coordinates
                .filter { pack(it) !in foundKeys }
                .forEach { coords ->
                    resolved.add(MavenCoordinateDTO.fromEntity(insertOrLookup(coords)))
                }
        }

        return resolved
    }

    /**
     * Resolves a single [MavenCoordinateDTO] to its persisted [MavenCoordinateDTO].
     * Returns the existing row or inserts a fresh one.
     */
    fun resolveOrCreate(coords: MavenCoordinateDTO): MavenCoordinateDTO {
        val existing = mavenCoordinateRepository.findByGroupIdAndArtifactIdAndVersion(
            coords.groupId, coords.artifactId, coords.version,
        )
        return MavenCoordinateDTO.fromEntity(existing ?: insertOrLookup(coords))
    }

    /**
     * Inserts the coordinate row via `saveIfAbsent`, falling back to a lookup
     * if the row already existed (concurrent insert). In either case,
     * reads and returns the row.
     */
    private fun insertOrLookup(coords: MavenCoordinateDTO): MavenCoordinateEntity {
        val inserted = mavenCoordinateRepository.saveIfAbsent(
            coords.groupId, coords.artifactId, coords.version,
        )
        if (inserted == 0L) {
            log.debug("maven_coordinate row already exists for coordinate: {}", coords)
        }
        return requireNotNull(
            mavenCoordinateRepository.findByGroupIdAndArtifactIdAndVersion(
                coords.groupId, coords.artifactId, coords.version,
            )
        ) {
            "Failed to resolve maven_coordinate row after insert: $coords"
        }
    }

    private companion object {
        private fun pack(coords: MavenCoordinateDTO): String =
            "${coords.groupId}|${coords.artifactId}|${coords.version}"

        private fun pack(entity: MavenCoordinateEntity): String =
            "${entity.groupId}|${entity.artifactId}|${entity.version}"
    }
}
