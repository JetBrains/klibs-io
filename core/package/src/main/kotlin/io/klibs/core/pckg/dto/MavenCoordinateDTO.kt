package io.klibs.core.pckg.dto

import io.klibs.core.pckg.entity.MavenCoordinateEntity

/**
 * Data Transfer Object for Maven coordinates.
 */
data class MavenCoordinateDTO(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val id: Long? = null,
) {

    fun toEntityRef(): MavenCoordinateEntity = MavenCoordinateEntity(
        id = id,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
    )

    companion object {
        fun fromEntity(entity: MavenCoordinateEntity): MavenCoordinateDTO {
            return MavenCoordinateDTO(
                id = requireNotNull(entity.id) {
                    "Cannot create MavenCoordinateDTO from a non-persisted entity: $entity"
                },
                groupId = entity.groupId,
                artifactId = entity.artifactId,
                version = entity.version,
            )
        }
    }
}
