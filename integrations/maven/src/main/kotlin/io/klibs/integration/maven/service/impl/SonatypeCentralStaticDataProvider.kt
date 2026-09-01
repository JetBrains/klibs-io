package io.klibs.integration.maven.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import kotlin.time.Instant
import kotlin.time.Clock
import org.apache.maven.search.api.transport.Java11HttpClientTransport
import org.apache.maven.search.api.transport.Transport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SonatypeCentralStaticDataProvider(
    xmlMapper: XmlMapper,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    objectMapper: ObjectMapper,
    @Value("\${klibs.integration.maven.central.content-endpoint}")
    contentEndpoint: String,
    @Value("\${klibs.integration.maven.central.content-fallback-endpoint}")
    contentFallbackEndpoint: String,
    clientTransport: Transport = Java11HttpClientTransport(),
    clock: Clock = Clock.System,
) : BaseMavenCentralStaticDataProvider(
    xmlMapper,
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(SonatypeCentralStaticDataProvider::class.java),
    objectMapper,
    contentEndpoint,
    contentFallbackEndpoint,
    "last-modified",
    clientTransport,
    clock
) {
    override val scraperType: ScraperType
        get() = ScraperType.CENTRAL_SONATYPE

    override fun parseReleasedAt(value: String): Instant = parseRfc1123Instant(value)
}