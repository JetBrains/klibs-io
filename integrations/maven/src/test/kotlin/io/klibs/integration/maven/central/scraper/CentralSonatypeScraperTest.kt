package io.klibs.integration.maven.central.scraper

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.scraper.impl.CentralSonatypeScraper
import io.klibs.integration.maven.search.MavenSearchClient
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.search.MavenSearchResponse
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.apache.maven.search.api.request.BooleanQuery
import org.apache.maven.search.api.request.Query
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.ArrayDeque
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CentralSonatypeScraperTest {

    private lateinit var fakeCentralClient: RecordingClient
    private lateinit var fakeDiscoveryClient: RecordingClient
    private lateinit var centralSonatypeScraper: MavenCentralScraper
    private lateinit var errorChannel: Channel<Exception>
    
    private class RecordingClient(
        private val pageSizeVal: Int = 2,
        responses: List<MavenSearchResponse> = emptyList(),
        private val throwOnCall: Boolean = false,
    ) : MavenSearchClient {
        val capturedPages = mutableListOf<Int>()
        val capturedQueries = mutableListOf<Query>()
        private val queue = ArrayDeque(responses)
        override fun pageSize(): Int = pageSizeVal
        override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse {
            if (throwOnCall) throw RuntimeException("Test exception")
            capturedPages += page
            capturedQueries += query
            return if (queue.isEmpty()) MavenSearchResponse(0, 0, emptyList()) else queue.removeFirst()
        }
    }

    @BeforeEach
    fun setUp() {
        fakeCentralClient = RecordingClient(pageSizeVal = 2)
        fakeDiscoveryClient = RecordingClient(pageSizeVal = 2)
        centralSonatypeScraper = CentralSonatypeScraper(fakeDiscoveryClient, object : CentralSonatypeSearchClient {
            override fun pageSize(): Int = fakeCentralClient.pageSize()
            override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse =
                fakeCentralClient.searchWithThrottle(page, query, lastUpdatedSince)
        })
        errorChannel = Channel(Channel.UNLIMITED)
    }

    @Test
    fun `test findAllVersionForArtifact returns all versions of an artifact`() = runTest {
        // Arrange
        val artifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "example-artifact",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = Instant.now()
        )

        val mockResponse = MavenSearchResponse(
            totalHits = 3,
            currentHits = 3,
            page = listOf(
                ArtifactData("org.example", "example-artifact", "1.0.0", Instant.ofEpochMilli(1000L)),
                ArtifactData("org.example", "example-artifact", "1.1.0", Instant.ofEpochMilli(2000L)),
                ArtifactData("org.example", "example-artifact", "1.2.0", Instant.ofEpochMilli(3000L))
            )
        )
        fakeCentralClient = RecordingClient(responses = listOf(mockResponse, MavenSearchResponse(0, 0, emptyList())))
        // rewire scraper to use the prepared central client
        centralSonatypeScraper = CentralSonatypeScraper(fakeDiscoveryClient, object : CentralSonatypeSearchClient {
            override fun pageSize(): Int = fakeCentralClient.pageSize()
            override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse =
                fakeCentralClient.searchWithThrottle(page, query, lastUpdatedSince)
        })

        // Act
        val result = centralSonatypeScraper.findAllVersionForArtifact(artifact, errorChannel).toList()

        val query = fakeCentralClient.capturedQueries.first()
        assertTrue(query is BooleanQuery.And, "Query should be a BooleanQuery.And")

        // Check that the query contains the expected parts
        assertTrue(
            query.left.value.contains("g:org.example") || query.right.value.contains("g:org.example"),
            "Query should include group ID"
        )
        assertTrue(
            query.left.value.contains("a:example-artifact") || query.right.value.contains("a:example-artifact"),
            "Query should include artifact ID"
        )

        // Verify the result
        assertEquals(3, result.size)
        assertEquals("1.0.0", result[0].version)
        assertEquals("1.1.0", result[1].version)
        assertEquals("1.2.0", result[2].version)
    }

    @Test
    fun `test findKmpArtifacts returns artifacts from search response`() = runTest {
        // Arrange
        val artifactData = ArtifactData("org.example", "example-artifact", "1.0.0", Instant.ofEpochMilli(1000L))
        val mockResponse = MavenSearchResponse(1, 1, listOf(artifactData))
        fakeDiscoveryClient = RecordingClient(responses = listOf(mockResponse, MavenSearchResponse(0, 0, emptyList())))
        centralSonatypeScraper = CentralSonatypeScraper(fakeDiscoveryClient, object : CentralSonatypeSearchClient {
            override fun pageSize(): Int = fakeCentralClient.pageSize()
            override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse =
                fakeCentralClient.searchWithThrottle(page, query, lastUpdatedSince)
        })

        // Act
        val result = centralSonatypeScraper.findKmpArtifacts(Instant.EPOCH, errorChannel).toList()

        // Verify the search request contains the expected query
        val query = fakeDiscoveryClient.capturedQueries.first()
        assertTrue(
            query.value.contains("l:kotlin-tooling-metadata"),
            "Query should search for kotlin-tooling-metadata"
        )

        // Verify the result
        assertEquals(1, result.size)
        assertEquals("org.example", result[0].groupId)
        assertEquals("example-artifact", result[0].artifactId)
        assertEquals("1.0.0", result[0].version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, result[0].scraperType)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test error handling when search client throws exception`() = runTest {
        fakeDiscoveryClient = RecordingClient(throwOnCall = true)
        centralSonatypeScraper = CentralSonatypeScraper(fakeDiscoveryClient, object : CentralSonatypeSearchClient {
            override fun pageSize(): Int = fakeCentralClient.pageSize()
            override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse =
                fakeCentralClient.searchWithThrottle(page, query, lastUpdatedSince)
        })

        // Act
        val result = centralSonatypeScraper.findKmpArtifacts(Instant.EPOCH, errorChannel).toList()
        val errors = mutableListOf<Exception>()
        while (!errorChannel.isEmpty) {
            errors.add(errorChannel.receive())
        }

        // Verify error handling
        assertTrue(result.isEmpty())
        assertTrue(errors.isNotEmpty())
        val message = errors[0].message ?: ""
        assertTrue(message.startsWith("Could not process request for artifacts:"))
        assertTrue(message.contains("l:kotlin-tooling-metadata"))
    }

    @Test
    fun `test findKmpArtifacts handles multiple pages of results`() = runTest {
        val page1Data = listOf(
            ArtifactData("org.example", "example-artifact1", "1.0.0", Instant.ofEpochMilli(1000L)),
            ArtifactData("org.example", "example-artifact2", "1.0.0", Instant.ofEpochMilli(2000L))
        )
        val page1Response = MavenSearchResponse(4, 2, page1Data)

        val page2Data = listOf(
            ArtifactData("org.example", "example-artifact3", "1.0.0", Instant.ofEpochMilli(3000L)),
            ArtifactData("org.example", "example-artifact4", "1.0.0", Instant.ofEpochMilli(4000L))
        )
        val page2Response = MavenSearchResponse(4, 2, page2Data)

        fakeDiscoveryClient = RecordingClient(
            pageSizeVal = 2,
            responses = listOf(page1Response, page2Response, MavenSearchResponse(0, 0, emptyList()))
        )
        centralSonatypeScraper = CentralSonatypeScraper(fakeDiscoveryClient, object : CentralSonatypeSearchClient {
            override fun pageSize(): Int = fakeCentralClient.pageSize()
            override fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant): MavenSearchResponse =
                fakeCentralClient.searchWithThrottle(page, query, lastUpdatedSince)
        })

        // Act
        val result = centralSonatypeScraper.findKmpArtifacts(Instant.EPOCH, errorChannel).toList()

        val pageValues = fakeDiscoveryClient.capturedPages
        assertTrue(
            pageValues[1] > pageValues[0],
            "Second request should have higher page offset"
        )

        // Verify we got results from both pages
        assertEquals(4, result.size, "Should have received 4 artifacts in total")

        // Verify the content of the results
        assertEquals("example-artifact1", result[0].artifactId)
        assertEquals("example-artifact2", result[1].artifactId)
        assertEquals("example-artifact3", result[2].artifactId)
        assertEquals("example-artifact4", result[3].artifactId)
    }

}
