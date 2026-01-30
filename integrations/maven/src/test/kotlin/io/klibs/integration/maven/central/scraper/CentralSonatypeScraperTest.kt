package io.klibs.integration.maven.central.scraper

import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.dto.MavenMetadata
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.scraper.impl.CentralSonatypeScraper
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchClient
import io.klibs.integration.maven.search.MavenSearchResponse
import io.klibs.integration.maven.search.impl.BaseMavenSearchClient
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.apache.maven.search.api.request.Query
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CentralSonatypeScraperTest {

    private lateinit var mockCentralSonatypeClient: BaseMavenSearchClient
    private lateinit var mockDiscoveryClient: MavenSearchClient
    private lateinit var centralSonatypeScraper: MavenCentralScraper
    private lateinit var errorChannel: Channel<Exception>
    private val queryCaptor = argumentCaptor<Query>()

    @BeforeEach
    fun setUp() {
        mockCentralSonatypeClient = mock<CentralSonatypeSearchClient>()
        mockDiscoveryClient = mock()
        centralSonatypeScraper =
            CentralSonatypeScraper(mockDiscoveryClient, mockCentralSonatypeClient as CentralSonatypeSearchClient)
        errorChannel = Channel(Channel.UNLIMITED)
    }

    @Test
    fun `test findKmpArtifacts returns artifacts from search response`() = runTest {
        // Arrange
        val artifactData = ArtifactData("org.example", "example-artifact", "1.0.0", Instant.ofEpochMilli(1000L))
        val mockResponse = MavenSearchResponse(1, 1, listOf(artifactData))
        whenever(mockDiscoveryClient.searchWithThrottle(any(), any(), any()))
            .thenReturn(mockResponse)
            .thenReturn(MavenSearchResponse(0, 0, emptyList()))

        // Act
        val result = centralSonatypeScraper.findKmpArtifacts(Instant.EPOCH, errorChannel).toList()

        // Verify the search request contains the expected query
        val queryCaptor = argumentCaptor<Query>()
        verify(mockDiscoveryClient, times(2)).searchWithThrottle(any(), queryCaptor.capture(), any())
        val query = queryCaptor.firstValue
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
        whenever(
            mockDiscoveryClient.searchWithThrottle(
                any(),
                any(),
                any()
            )
        ).thenThrow(RuntimeException("Test exception"))

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

        whenever(mockDiscoveryClient.searchWithThrottle(any(), any(), any()))
            .thenReturn(page1Response)
            .thenReturn(page2Response)
            .thenReturn(MavenSearchResponse(0, 0, emptyList()))

        whenever(mockDiscoveryClient.pageSize()).thenReturn(2)

        // Act
        val result = centralSonatypeScraper.findKmpArtifacts(Instant.EPOCH, errorChannel).toList()

        val pageCaptor = argumentCaptor<Int>()
        val queryCaptor2 = argumentCaptor<Query>()
        verify(mockDiscoveryClient, times(3)).searchWithThrottle(pageCaptor.capture(), queryCaptor2.capture(), any())
        assertTrue(
            pageCaptor.allValues[1] > pageCaptor.allValues[0],
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

    @Test
    fun `test findNewVersions returns only new versions not in known set`() = runTest {
        // Arrange
        val knownArtifacts = mapOf(
            "org.example:example-artifact" to setOf("1.0.0", "1.1.0")
        )

        val metadata = MavenMetadata(
            groupId = "org.example",
            artifactId = "example-artifact",
            versioning = MavenMetadata.Versioning(
                latest = "1.3.0",
                release = "1.3.0",
                versions = listOf("1.0.0", "1.1.0", "1.2.0", "1.3.0"),
                lastUpdated = "20240101120000"
            )
        )

        whenever(mockCentralSonatypeClient.getMavenMetadata("org.example", "example-artifact"))
            .thenReturn(metadata)

        // Act
        val result = centralSonatypeScraper.findNewVersions(knownArtifacts, errorChannel).toList()

        // Verify
        assertEquals(2, result.size, "Should return only new versions")
        assertEquals("1.2.0", result[0].version)
        assertEquals("1.3.0", result[1].version)
        assertEquals("org.example", result[0].groupId)
        assertEquals("example-artifact", result[0].artifactId)
        assertEquals(ScraperType.CENTRAL_SONATYPE, result[0].scraperType)
        assertNull(result[0].releasedAt, "ReleasedAt should be null for new versions during discovery")
        assertNull(result[1].releasedAt, "ReleasedAt should be null for new versions during discovery")
    }

    @Test
    fun `test findNewVersions handles invalid coordinates format`() = runTest {
        // Arrange
        val knownArtifacts = mapOf(
            "invalid-format" to setOf("1.0.0"),
            "org.example:example-artifact" to setOf("1.0.0")
        )

        val metadata = MavenMetadata(
            groupId = "org.example",
            artifactId = "example-artifact",
            versioning = MavenMetadata.Versioning(
                latest = "1.1.0",
                release = "1.1.0",
                versions = listOf("1.0.0", "1.1.0"),
                lastUpdated = null
            )
        )

        whenever(mockCentralSonatypeClient.getMavenMetadata("org.example", "example-artifact"))
            .thenReturn(metadata)

        // Act
        val result = centralSonatypeScraper.findNewVersions(knownArtifacts, errorChannel).toList()

        // Verify - should only return valid artifact
        assertEquals(1, result.size, "Should skip invalid coordinates")
        assertEquals("1.1.0", result[0].version)
        assertNull(result[0].releasedAt)
    }

    @Test
    fun `test findNewVersions handles null metadata`() = runTest {
        // Arrange
        val knownArtifacts = mapOf(
            "org.example:example-artifact" to setOf("1.0.0")
        )

        whenever(mockCentralSonatypeClient.getMavenMetadata("org.example", "example-artifact"))
            .thenReturn(null)

        // Act
        val result = centralSonatypeScraper.findNewVersions(knownArtifacts, errorChannel).toList()

        // Verify
        assertEquals(0, result.size, "Should return empty list when metadata is null")
    }

    @Test
    fun `test findNewVersions handles exception and sends to error channel`() = runTest {
        // Arrange
        val knownArtifacts = mapOf(
            "org.example:example-artifact" to setOf("1.0.0")
        )

        whenever(mockCentralSonatypeClient.getMavenMetadata("org.example", "example-artifact"))
            .thenThrow(RuntimeException("Network error"))

        // Act
        val result = centralSonatypeScraper.findNewVersions(knownArtifacts, errorChannel).toList()
        val errors = mutableListOf<Exception>()
        while (!errorChannel.isEmpty) {
            errors.add(errorChannel.receive())
        }

        // Verify
        assertEquals(0, result.size, "Should return empty list on error")
        assertEquals(1, errors.size, "Should send error to error channel")
        assertTrue(errors[0].message?.contains("Could not process request for metadata") == true)
        assertTrue(errors[0].message?.contains("org.example:example-artifact") == true)
    }

}
