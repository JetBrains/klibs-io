package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.androidx.ModuleMetadataWrapper
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.dto.MavenMetadata
import java.time.Instant
import org.apache.maven.model.Dependency
import org.apache.maven.model.License
import org.apache.maven.model.Model
import org.apache.maven.model.Scm

class MavenPom(private val model: Model) {

    val groupId: String
        get() = model.groupId ?: model.parent?.groupId ?: error("No groupId found for ${scm?.url}")

    val version: String
        get() = model.version ?: model.parent?.version ?: error("No version found for ${scm?.url}")

    val artifactId: String get() = model.artifactId ?: error("No artifactId found for ${scm?.url}")
    val description: String? get() = model.description
    val url: String? get() = model.url
    val scm: Scm? get() = model.scm
    val developers get() = model.developers ?: emptyList()
    val licenses: List<License> get() = model.licenses ?: emptyList()
    val dependencies: List<Dependency> get() = model.dependencies ?: emptyList()
}

data class PomWithReleaseDate(val pom: MavenPom, val releasedAt: Instant)

interface MavenStaticDataProvider {

    val scraperType: ScraperType

    fun getPom(mavenArtifact: MavenArtifact): MavenPom?

    fun getPomWithReleaseDate(mavenArtifact: MavenArtifact): PomWithReleaseDate?

    fun getPomUrl(mavenArtifact: MavenArtifact): String

    fun getKotlinToolingMetadata(mavenArtifact: MavenArtifact): KotlinToolingMetadataDelegate?

    fun getMavenMetadata(groupId: String, artifactId: String): MavenMetadata?

    /**
     * Gets metadata for a specific artifact.
     *
     * @param groupId The group ID of the artifact
     * @param artifactId The artifact ID
     * @param version The version of the artifact
     * @return Result containing ArtifactMetadata if successful, or an error if failed
     */
    fun getModuleMetadata(
        groupId: String,
        artifactId: String,
        version: String
    ): ModuleMetadataWrapper?
}
