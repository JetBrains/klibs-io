package io.klibs.core.pckg.dto

import io.klibs.core.pckg.entity.MavenArtifactEntity

/**
 * Service-layer Data Transfer Object for a `maven_artifact` row.
 */
data class MavenArtifactDTO(
    val id: Long,
    val groupId: String,
    val artifactId: String,
    val version: String,
) {

    fun toEntityRef(): MavenArtifactEntity = MavenArtifactEntity(
        id = id,
        groupId = groupId,
        artifactId = artifactId,
        version = version,
    )

    companion object {
        fun fromEntity(entity: MavenArtifactEntity): MavenArtifactDTO {
            return MavenArtifactDTO(
                id = requireNotNull(entity.id) {
                    "Cannot create MavenArtifactDTO from a non-persisted entity: $entity"
                },
                groupId = entity.groupId,
                artifactId = entity.artifactId,
                version = entity.version,
            )
        }
    }
}
