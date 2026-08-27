package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component("GOOGLE_MAVEN_CENTRAL_MIRROR")
@ConditionalOnProperty("klibs.indexing-configuration.google-maven-central-mirror.enabled", havingValue = "true")
class GoogleMavenCentralMirrorSearchClient(
    xmlMapper: XmlMapper,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    objectMapper: ObjectMapper,
    @Value("\${klibs.integration.maven.central.content-endpoint}")
    contentEndpoint: String,
    @Value("\${klibs.integration.maven.central.content-fallback-endpoint}")
    contentFallbackEndpoint: String,
) : BaseCentralMavenSearchClient(
    xmlMapper,
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(GoogleMavenCentralMirrorSearchClient::class.java),
    objectMapper,
    contentEndpoint,
    contentFallbackEndpoint,
    "x-goog-meta-last-modified-epoch"
)