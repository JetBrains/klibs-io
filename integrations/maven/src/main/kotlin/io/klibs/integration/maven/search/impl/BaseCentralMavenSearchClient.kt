package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import org.slf4j.Logger

abstract class BaseCentralMavenSearchClient(
    xmlMapper: XmlMapper,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    logger: Logger,
    objectMapper: ObjectMapper,
    private val contentEndpoint: String,
    private val contentFallbackEndpoint: String,
    lastModifiedHeader: String,
) : BaseMavenSearchClient(
    xmlMapper = xmlMapper,
    rateLimiter = mavenCentralRateLimiter,
    logger = logger,
    objectMapper = objectMapper,
    lastModifiedHeader = lastModifiedHeader
) {

    override fun getContentUrlPrefix(): String = contentEndpoint

    override fun getContentFallbackUrlPrefix(): String = contentFallbackEndpoint

}
