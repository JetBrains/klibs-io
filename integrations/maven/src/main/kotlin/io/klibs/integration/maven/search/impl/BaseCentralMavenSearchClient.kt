package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchResponse
import java.time.Instant
import org.apache.maven.search.api.MAVEN
import org.apache.maven.search.api.Record
import org.apache.maven.search.api.SearchRequest
import org.apache.maven.search.api.request.Paging
import org.apache.maven.search.api.request.Query
import org.apache.maven.search.backend.smo.SmoSearchBackend
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.CSC_BACKEND_ID
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.CSC_REPOSITORY_ID
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.CSC_SMO_URI
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
    private val searchClient: SmoSearchBackend = SmoSearchBackendFactory.create(
        CSC_BACKEND_ID,
        CSC_REPOSITORY_ID,
        CSC_SMO_URI,
        clientTransport
    )

    override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse {
        val paging = Paging(pageSize(), page)
        val request = SearchRequest(paging, query)
        request.nextPage()

        val response = executeWithThrottle {
            rateLimiter.withRateLimitBlocking {
                searchClient.search(request)
            }
        }

        return MavenSearchResponse(
            totalHits = response.totalHits,
            currentHits = response.currentHits,
            page = response.page.map { it.toArtifactData() },
        )
    }

    override fun getContentUrlPrefix(): String = contentEndpoint

    override fun getContentFallbackUrlPrefix(): String = contentFallbackEndpoint

    private fun Record.toArtifactData(): ArtifactData {
        return ArtifactData(
            groupId = this.getValue(MAVEN.GROUP_ID),
            artifactId = this.getValue(MAVEN.ARTIFACT_ID),
            version = this.getValue(MAVEN.VERSION),
            releasedAt = Instant.ofEpochMilli(this.lastUpdated)
        )
    }
}
