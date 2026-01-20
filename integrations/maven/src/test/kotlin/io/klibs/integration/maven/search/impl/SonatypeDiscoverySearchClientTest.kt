package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SonatypeDiscoverySearchClientTest {

    @Test
    fun `searchWithThrottle returns only packages discovered strictly after lastUpdatedSince`() {
        // Given
        val discoveryEndpoint = "https://example.com/api/discovery"
        val properties = MavenIntegrationProperties(
            MavenIntegrationProperties.Central(
                rateLimitCapacity = 100,
                rateLimitRefillAmount = 100,
                rateLimitRefillPeriodSec = 1,
                discoveryEndpoint = discoveryEndpoint,
                searchEndpoint = "https://example.com/api/search"
            )
        )
        val rateLimiter = MavenCentralRateLimiter(properties, SimpleMeterRegistry())

        val cutoff = Instant.ofEpochMilli(2_000L)
        val beforeTs = 1_000L
        val equalTs = 2_000L
        val afterTs = 3_000L

        val componentSearchResults = """
            {
              "components": [
                {
                  "namespace": "com.example",
                  "name": "lib-before",
                  "latestVersionInfo": { "version": "1.0.0", "timestampUnixWithMS": $beforeTs }
                },
                {
                  "namespace": "com.example",
                  "name": "lib-equal",
                  "latestVersionInfo": { "version": "1.0.1", "timestampUnixWithMS": $equalTs }
                },
                {
                  "namespace": "com.example",
                  "name": "lib-after",
                  "latestVersionInfo": { "version": "1.0.2", "timestampUnixWithMS": $afterTs }
                }
              ],
              "page": 0,
              "pageSize": 20,
              "pageCount": 1,
              "totalResultCount": 3
            }
        """.trimIndent()

        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(ExpectedCount.once(), requestTo(discoveryEndpoint))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(componentSearchResults, MediaType.APPLICATION_JSON))

        val objectMapper = ObjectMapper().findAndRegisterModules()
        val client = SonatypeDiscoverSearchClient(properties, rateLimiter, objectMapper, builder)

        // We only need a Query with a value, so mock it
        val query = mockk<Query>()
        every { query.value } returns "some-query"

        // When
        val result: MavenSearchResponse = client.searchWithThrottle(page = 0, query = query, lastUpdatedSince = cutoff)

        // Then
        assertEquals(1, result.currentHits)
        assertEquals(1, result.page.size)
        val only = result.page.first()
        assertEquals("com.example", only.groupId)
        assertEquals("lib-after", only.artifactId)
        assertEquals("1.0.2", only.version)
        assertTrue(only.releasedAt!!.isAfter(cutoff), "releasedAt should be strictly after cutoff")

        server.verify()
    }
}
