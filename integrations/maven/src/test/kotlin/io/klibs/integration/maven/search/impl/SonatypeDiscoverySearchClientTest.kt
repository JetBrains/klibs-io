package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
        val properties = mock<MavenIntegrationProperties>()
        val central = mock<MavenIntegrationProperties.Central>()
        whenever(properties.central).thenReturn(central)
        whenever(central.discoveryEndpoint).thenReturn(discoveryEndpoint)

        val rateLimiter = mock<MavenCentralRateLimiter>()
        // Make the rate limiter just run the action immediately
        whenever(rateLimiter.withRateLimitBlocking<Any>(any())).thenAnswer { invocation ->
            val action = invocation.arguments[0] as () -> Any
            action.invoke()
        }

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
        val query = mock<Query>()
        whenever(query.value).thenReturn("some-query")

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
