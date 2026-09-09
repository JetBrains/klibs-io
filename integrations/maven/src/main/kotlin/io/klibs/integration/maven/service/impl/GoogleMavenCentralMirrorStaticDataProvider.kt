package io.klibs.integration.maven.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import kotlin.time.Clock
import kotlin.time.Instant
import org.apache.maven.search.api.transport.Java11HttpClientTransport
import org.apache.maven.search.api.transport.Transport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleMavenCentralMirrorStaticDataProvider(
    xmlMapper: XmlMapper,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    objectMapper: ObjectMapper,
    @Value("\${klibs.integration.maven.google-maven-central-mirror.content-endpoint}")
    contentEndpoint: String,
    clientTransport: Transport = Java11HttpClientTransport(),
    clock: Clock = Clock.System,
) : BaseMavenCentralStaticDataProvider(
    xmlMapper,
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(GoogleMavenCentralMirrorStaticDataProvider::class.java),
    objectMapper,
    contentEndpoint,
    clientTransport,
    clock
) {
    override val scraperType: ScraperType
        get() = ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR

    override fun parseReleasedAt(value: Map<String, String>): Instant {
        val epoch = value.entries
            .firstOrNull { it.key.equals("x-goog-meta-last-modified-epoch", ignoreCase = true) }
            ?.value
        val lastModified = epoch ?: value.entries
            .firstOrNull { it.key.equals("x-goog-meta-mtime", ignoreCase = true) }
            ?.value
            ?: throw IllegalStateException(
                "Missing release date header: expected x-goog-meta-last-modified-epoch or x-goog-meta-mtime"
            )

        return try {
            if (epoch != null) parseEpochMillisecondsInstant(lastModified) else Instant.parse(lastModified)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid release date format: $lastModified", e)
        }
    }
}