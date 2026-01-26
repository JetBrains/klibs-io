package io.klibs.integration.maven

import io.klibs.integration.maven.androidx.ModuleMetadataWrapper
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.dto.MavenMetadata
import java.time.Instant

typealias MavenPom = org.apache.maven.model.Model

interface MavenStaticDataProvider {
    fun getPom(mavenArtifact: MavenArtifact): MavenPom?

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

    fun getReleaseDate(
        groupId: String,
        artifactId: String,
        version: String
    ): Instant?
}
