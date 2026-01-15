package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchClient
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant

@Component("CENTRAL_SONATYPE_DISCOVERY")
@Primary
class SonatypeDiscoverSearchClient(
    private val properties: MavenIntegrationProperties,
    private val rateLimiter: MavenCentralRateLimiter,
    private val objectMapper: ObjectMapper,
    private val restClientBuilder: RestClient.Builder
): MavenSearchClient {

    private val logger = LoggerFactory.getLogger(SonatypeDiscoverSearchClient::class.java)
    private val restClient: RestClient by lazy { restClientBuilder.build() }

    override fun pageSize(): Int = DEFAULT_PAGE_SIZE

    override fun searchWithThrottle(
        page: Int,
        query: Query,
        lastUpdatedSince: Instant
    ): MavenSearchResponse {
        val searchTerm = query.value

        val payloadObj = BrowseRequest(
            page = page,
            size = DEFAULT_PAGE_SIZE,
            searchTerm = searchTerm,
            sortField = "publishedDate",
            sortDirection = "desc",
            filter = emptyList()
        )

        val responseBody = try {
            rateLimiter.withRateLimitBlocking {
                val response = restClient.post()
                    .uri(properties.central.discoveryEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payloadObj)
                    .retrieve()
                    .toEntity(String::class.java)
                if (!response.statusCode.is2xxSuccessful) {
                    throw IllegalStateException("Unexpected response ${response.statusCode.value()} from Sonatype discovery API: ${response.body}")
                }
                response.body ?: ""
            }
        } catch (e: Exception) {
            logger.error("Failed to query Sonatype Discovery API", e)
            throw e
        }

        val parsed = objectMapper.readValue(responseBody, BrowseResponse::class.java)
        val artifacts = parsed.components
            .mapNotNull { it.toArtifactData() }
            .filter { it.releasedAt?.isAfter(lastUpdatedSince) == true }

        return MavenSearchResponse(
            totalHits = -1,
            currentHits = artifacts.size,
            page = artifacts
        )
    }

    private fun BrowseResponse.Component.toArtifactData(): ArtifactData? {
        val g = namespace
        val a = artifactId
        val v = latestVersionInfo?.version
        if (g == null || a == null || v == null) return null

        val releasedAt = latestVersionInfo.timestampUnixWithMS?.let { Instant.ofEpochMilli(it) }
        return ArtifactData(
            groupId = g,
            artifactId = a,
            version = v,
            releasedAt = releasedAt
        )
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrowseRequest(
    val page: Int,
    val size: Int,
    val searchTerm: String,
    val sortField: String,
    val sortDirection: String,
    val filter: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrowseResponse(
    val components: List<Component> = emptyList(),
    val page: Int,
    val pageSize: Int,
    val pageCount: Int,
    val totalResultCount: Int,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Component(
        val id: String? = null,
        val namespace: String? = null,
        @JsonProperty("name")
        val artifactId: String? = null,
        val packaging: String? = null,
        val classifiers: List<String>? = null,
        val latestVersionInfo: LatestVersionInfo? = null,
        val publishedEpochMillis: Long? = null,
        val description: String? = null,
        val projectName: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LatestVersionInfo(
        val version: String? = null,
        @JsonProperty("timestampUnixWithMS")
        val timestampUnixWithMS: Long? = null,
        val licenses: List<String>? = null
    )
}