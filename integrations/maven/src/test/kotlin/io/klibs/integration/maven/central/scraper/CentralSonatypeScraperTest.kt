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
    private lateinit var centralSonatypeScraper: MavenCentralScraper
    private lateinit var errorChannel: Channel<Exception>

    @BeforeEach
    fun setUp() {
        mockCentralSonatypeClient = mock<CentralSonatypeSearchClient>()
        centralSonatypeScraper =
            CentralSonatypeScraper(mockCentralSonatypeClient as CentralSonatypeSearchClient)
        errorChannel = Channel(Channel.UNLIMITED)
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
