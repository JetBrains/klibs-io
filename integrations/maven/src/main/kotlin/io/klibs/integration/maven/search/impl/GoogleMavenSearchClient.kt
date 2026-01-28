package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.request.RequestRateLimiter
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

const val GOOGLE_MAVEN_URL = "https://dl.google.com/dl/android/maven2/"

@Component("GOOGLE_MAVEN")
class GoogleMavenSearchClient(
    xmlMapper: XmlMapper,
    unlimitedRateLimiter: RequestRateLimiter,
    objectMapper: ObjectMapper
) : BaseMavenSearchClient(xmlMapper, unlimitedRateLimiter, logger, objectMapper) {

    companion object {
        val logger = LoggerFactory.getLogger(GoogleMavenSearchClient::class.java)
    }

    override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse {
        throw UnsupportedOperationException("Google Maven does not support searching API.")
    }

    override fun getContentUrlPrefix(): String {
        return GOOGLE_MAVEN_URL
    }

    override fun getKotlinToolingMetadata(mavenArtifact: MavenArtifact): KotlinToolingMetadataDelegate? {
        try {
            super.getKotlinToolingMetadata(mavenArtifact)
        } catch (e: Throwable) {
            logger.debug("Failed to find kotlin-tooling-metadata.json file for: {}", mavenArtifact)

        }

        val moduleMetadata = getModuleMetadata(mavenArtifact.groupId, mavenArtifact.artifactId, mavenArtifact.version) ?: return null
        return convertModuleToToolingMetadata(moduleMetadata.gradleMetadata)
    }


    private fun convertModuleToToolingMetadata(metadata: GradleMetadata): KotlinToolingMetadataDelegate {
        return KotlinToolingMetadataDelegateStubImpl(metadata)
    }
}