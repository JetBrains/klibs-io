package io.klibs.integration.maven.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import kotlin.time.Clock
import org.apache.maven.search.api.transport.Java11HttpClientTransport
import org.apache.maven.search.api.transport.Transport
import org.slf4j.Logger

abstract class BaseMavenCentralStaticDataProvider(
    xmlMapper: XmlMapper,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    logger: Logger,
    objectMapper: ObjectMapper,
    private val contentEndpoint: String,
    private val contentFallbackEndpoint: String,
    lastModifiedHeader: String,
    clientTransport: Transport = Java11HttpClientTransport(),
    clock: Clock = Clock.System,
) : BaseMavenStaticDataProvider(
    xmlMapper = xmlMapper,
    rateLimiter = mavenCentralRateLimiter,
    logger = logger,
    objectMapper = objectMapper,
    clientTransport = clientTransport,
    clock = clock,
    lastModifiedHeader = lastModifiedHeader
) {

    override fun getContentUrlPrefix(): String = contentEndpoint

    override fun getContentFallbackUrlPrefix(): String = contentFallbackEndpoint

}
