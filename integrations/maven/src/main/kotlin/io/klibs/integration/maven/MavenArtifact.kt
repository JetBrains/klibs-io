package io.klibs.integration.maven

import java.time.Instant

data class MavenArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val scraperType: ScraperType,
    val releasedAt: Instant? = null
)
