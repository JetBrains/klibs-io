package io.klibs.integration.maven.dto

data class MavenArtifactDTO(
    val groupId: String,
    val artifactId: String,
    val version: String?
)