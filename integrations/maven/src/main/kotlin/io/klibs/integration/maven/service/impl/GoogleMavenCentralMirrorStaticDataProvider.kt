package io.klibs.integration.maven.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import kotlin.time.Clock
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
    @Value("\${klibs.integration.maven.google-maven-central-mirror.content-fallback-endpoint}")
    contentFallbackEndpoint: String,
    clientTransport: Transport = Java11HttpClientTransport(),
    clock: Clock = Clock.System,
) : BaseMavenCentralStaticDataProvider(
    xmlMapper,
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(GoogleMavenCentralMirrorStaticDataProvider::class.java),
    objectMapper,
    contentEndpoint,
    contentFallbackEndpoint,
    "x-goog-meta-last-modified-epoch",
    clientTransport,
    clock
) {
    override val scraperType: ScraperType
        get() = ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR

    override fun parseReleasedAt(value: String) = parseEpochMillisecondsInstant(value)
}