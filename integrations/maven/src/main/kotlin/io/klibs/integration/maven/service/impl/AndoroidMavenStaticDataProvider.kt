package io.klibs.integration.maven.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.request.RequestRateLimiter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

const val GOOGLE_MAVEN_URL = "https://dl.google.com/dl/android/maven2/"

@Component
class GoogleMavenStaticDataProvider(
    xmlMapper: XmlMapper,
    unlimitedRateLimiter: RequestRateLimiter,
    objectMapper: ObjectMapper
) : BaseMavenStaticDataProvider(
    xmlMapper = xmlMapper,
    rateLimiter = unlimitedRateLimiter,
    logger = logger,
    objectMapper = objectMapper,
) {
    override fun getContentUrlPrefix(): String {
        return GOOGLE_MAVEN_URL
    }

    override val scraperType: ScraperType
        get() = ScraperType.GOOGLE_MAVEN

    override fun getKotlinToolingMetadata(mavenArtifact: MavenArtifact): KotlinToolingMetadataDelegate? {
        try {
            super.getKotlinToolingMetadata(mavenArtifact)
        } catch (e: Throwable) {
            logger.debug("Failed to find kotlin-tooling-metadata.json file for: {}", mavenArtifact)

        }

        val moduleMetadata =
            getModuleMetadata(mavenArtifact.groupId, mavenArtifact.artifactId, mavenArtifact.version) ?: return null
        return convertModuleToToolingMetadata(moduleMetadata.gradleMetadata)
    }

    private fun convertModuleToToolingMetadata(metadata: GradleMetadata): KotlinToolingMetadataDelegate {
        return KotlinToolingMetadataDelegateStubImpl(metadata)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoogleMavenStaticDataProvider::class.java)
    }
}