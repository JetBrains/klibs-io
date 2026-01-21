package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchResponse
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

private const val MAVEN_CENTRAL_REPOSITORY_URL = "https://search.maven.org/remotecontent?filepath="

@Component("CENTRAL_SONATYPE")
class CentralSonatypeSearchClient(
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    objectMapper: ObjectMapper,
) : BaseMavenSearchClient(
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(CentralSonatypeSearchClient::class.java),
    objectMapper,
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

    override fun getContentUrlPrefix(): String {
        return MAVEN_CENTRAL_REPOSITORY_URL
    }

    private fun Record.toArtifactData(): ArtifactData {
        return ArtifactData(
            groupId = this.getValue(MAVEN.GROUP_ID),
            artifactId = this.getValue(MAVEN.ARTIFACT_ID),
            version = this.getValue(MAVEN.VERSION),
            releasedAt = Instant.ofEpochMilli(this.lastUpdated)
        )
    }
}