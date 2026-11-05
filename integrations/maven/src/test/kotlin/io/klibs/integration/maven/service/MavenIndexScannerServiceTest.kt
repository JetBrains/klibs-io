package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.ScraperType
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.maven.index.ArtifactInfo
import org.apache.maven.index.Indexer
import org.apache.maven.index.IteratorResultSet
import org.apache.maven.index.IteratorSearchResponse
import org.apache.maven.index.context.IndexingContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@ExtendWith(MockKExtension::class)
class MavenIndexScannerServiceTest {

    @TempDir
    lateinit var tempDir: File

    @MockK(relaxUnitFun = true)
    private lateinit var indexer: Indexer

    private lateinit var indexingContextManager: MavenIndexingContextManager

    @MockK
    private lateinit var indexingContext: IndexingContext

    @MockK(relaxUnitFun = true)
    private lateinit var iteratorSearchResponse: IteratorSearchResponse

    @MockK
    private lateinit var iteratorResultSet: IteratorResultSet

    private lateinit var uut: MavenIndexScannerService
    private lateinit var properties: MavenIntegrationProperties

    @BeforeEach
    fun setup() {
        properties = MavenIntegrationProperties(
            central = MavenIntegrationProperties.Central(
                rateLimitCapacity = 100,
                rateLimitRefillAmount = 10,
                rateLimitRefillPeriodSec = 1,
                indexDir = tempDir.absolutePath,
            )
        )
        indexingContextManager = MavenIndexingContextManager(properties, indexer, emptyList())
        uut = CentralSonatypeMavenIndexScannerService(indexer, indexingContextManager)

        every {
            indexer.createIndexingContext(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns indexingContext
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should scan and emit MavenArtifact objects`(repositoryUrl: String): Unit = runBlocking {
        val artifactInfo = ArtifactInfo().apply {
            groupId = "io.klibs"
            artifactId = "test-artifact"
            version = "1.0.0"
        }

        every { indexer.searchIterator(any()) } returns iteratorSearchResponse
        every { iteratorSearchResponse.iterator() } returns iteratorResultSet
        every { iteratorResultSet.hasNext() } returnsMany listOf(true, false)
        every { iteratorResultSet.next() } returns artifactInfo

        // When
        val result = uut.scanForNewKMPArtifacts(repositoryUrl).toList()

        // Then
        assertEquals(1, result.size)
        val artifact = result[0]
        assertEquals("io.klibs", artifact.groupId)
        assertEquals("test-artifact", artifact.artifactId)
        assertEquals("1.0.0", artifact.version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, artifact.scraperType)

        verify(exactly = 1) {
            indexer.searchIterator(withArg {
                assertEquals("+l:kotlin-tooling-metadata +p:json", it.query.toString())
                assertEquals(listOf(indexingContext), it.contexts.toList())
            })
        }
        verify(exactly = 1) { iteratorSearchResponse.close() }
        verifyIndexingContext(repositoryUrl)
    }

    @Test
    fun `should use mirror scraper type`(): Unit = runBlocking {
        uut = GoogleMavenCentralMirrorIndexScannerService(indexer, indexingContextManager)

        val artifactInfo = ArtifactInfo().apply {
            groupId = "io.klibs"
            artifactId = "test-artifact"
            version = "1.0.0"
        }

        every { indexer.searchIterator(any()) } returns iteratorSearchResponse
        every { iteratorSearchResponse.iterator() } returns iteratorResultSet
        every { iteratorResultSet.hasNext() } returnsMany listOf(true, false)
        every { iteratorResultSet.next() } returns artifactInfo

        val result = uut.scanForNewKMPArtifacts(GOOGLE_MIRROR_REPOSITORY_URL).toList()

        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, result.single().scraperType)
        verify(exactly = 1) { indexer.searchIterator(any()) }
        verify(exactly = 1) { iteratorSearchResponse.close() }
        verifyIndexingContext(GOOGLE_MIRROR_REPOSITORY_URL)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should handle empty search results`(repositoryUrl: String): Unit = runBlocking {
        every { indexer.searchIterator(any()) } returns iteratorSearchResponse
        every { iteratorSearchResponse.iterator() } returns iteratorResultSet
        every { iteratorResultSet.hasNext() } returns false

        // When
        val result = uut.scanForNewKMPArtifacts(repositoryUrl).toList()

        // Then
        assertEquals(0, result.size)
        verify(exactly = 1) { iteratorSearchResponse.close() }
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should handle exceptions during scanning`(repositoryUrl: String): Unit = runBlocking {
        val failure = RuntimeException("Search failed")
        every { indexer.searchIterator(any()) } throws failure

        // When & Then
        val thrown = assertFailsWith<RuntimeException> {
            uut.scanForNewKMPArtifacts(repositoryUrl).toList()
        }
        assertSame(failure, thrown)
        verify {
            iteratorSearchResponse wasNot Called
            iteratorResultSet wasNot Called
        }
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should scan lazily and close resources when collection stops early`(repositoryUrl: String): Unit = runBlocking {
        val artifactInfo = ArtifactInfo().apply {
            groupId = "io.klibs"
            artifactId = "first-artifact"
            version = "1.0.0"
        }

        every { indexer.searchIterator(any()) } returns iteratorSearchResponse
        every { iteratorSearchResponse.iterator() } returns iteratorResultSet
        every { iteratorResultSet.hasNext() } returnsMany listOf(true, true, false)
        every { iteratorResultSet.next() } returns artifactInfo

        val artifacts = uut.scanForNewKMPArtifacts(repositoryUrl)
        verify {
            indexer wasNot Called
            iteratorSearchResponse wasNot Called
            iteratorResultSet wasNot Called
        }

        val result = artifacts.take(1).toList()

        assertEquals("first-artifact", result.single().artifactId)
        verify(exactly = 1) { iteratorResultSet.next() }
        verify(exactly = 1) { iteratorSearchResponse.close() }
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should close search results and context when iteration fails`(repositoryUrl: String): Unit = runBlocking {
        val failure = IllegalStateException("Iteration failed")
        every { indexer.searchIterator(any()) } returns iteratorSearchResponse
        every { iteratorSearchResponse.iterator() } returns iteratorResultSet
        every { iteratorResultSet.hasNext() } returns true
        every { iteratorResultSet.next() } throws failure

        val thrown = assertFailsWith<IllegalStateException> {
            uut.scanForNewKMPArtifacts(repositoryUrl).toList()
        }

        assertSame(failure, thrown)
        verify(exactly = 1) { iteratorSearchResponse.close() }
        verifyIndexingContext(repositoryUrl)
    }

    private fun verifyIndexingContext(repositoryUrl: String) {
        val indexDir = File(tempDir, "maven-central-index")
        verify(exactly = 1) {
            indexer.createIndexingContext(
                "maven-central-scan-context",
                "central",
                indexDir,
                indexDir,
                repositoryUrl,
                null,
                true,
                true,
                emptyList(),
            )
        }
        verify(exactly = 1) { indexer.closeIndexingContext(indexingContext, false) }
    }

    companion object {
        private const val CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/"
        private const val GOOGLE_MIRROR_REPOSITORY_URL = "https://maven-central.storage-download.googleapis.com/maven2/"
    }
}
