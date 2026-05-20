package io.klibs.core.pckg.dto

/**
 * Maven coordinates uniquely identifying an artifact version.
 */
data class MavenCoordinatesDTO(
    val groupId: String,
    val artifactId: String,
    val version: String,
)