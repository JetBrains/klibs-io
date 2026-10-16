package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.ScraperType
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
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@ExtendWith(MockitoExtension::class)
class MavenIndexScannerServiceTest {

    @TempDir
    lateinit var tempDir: File

    @Mock
    private lateinit var indexer: Indexer

    private lateinit var indexingContextManager: MavenIndexingContextManager

    @Mock
    private lateinit var indexingContext: IndexingContext

    @Mock
    private lateinit var iteratorSearchResponse: IteratorSearchResponse

    @Mock
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

        whenever(indexer.createIndexingContext(any(), any(), any(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(indexingContext)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should scan and emit MavenArtifact objects`(repositoryUrl: String): Unit = runBlocking {
        val artifactInfo = ArtifactInfo().apply {
            groupId = "io.klibs"
            artifactId = "test-artifact"
            version = "1.0.0"
        }

        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(true, false)
        whenever(iteratorResultSet.next()).thenReturn(artifactInfo)

        // When
        val result = uut.scanForNewKMPArtifacts(repositoryUrl).toList()

        // Then
        assertEquals(1, result.size)
        val artifact = result[0]
        assertEquals("io.klibs", artifact.groupId)
        assertEquals("test-artifact", artifact.artifactId)
        assertEquals("1.0.0", artifact.version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, artifact.scraperType)

        verify(indexer).searchIterator(check {
            assertEquals("+l:kotlin-tooling-metadata +p:json", it.query.toString())
            assertEquals(listOf(indexingContext), it.contexts.toList())
        })
        verify(iteratorSearchResponse).close()
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

        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(true, false)
        whenever(iteratorResultSet.next()).thenReturn(artifactInfo)

        val result = uut.scanForNewKMPArtifacts(GOOGLE_MIRROR_REPOSITORY_URL).toList()

        assertEquals(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR, result.single().scraperType)
        verify(indexer).searchIterator(any())
        verify(iteratorSearchResponse).close()
        verifyIndexingContext(GOOGLE_MIRROR_REPOSITORY_URL)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should handle empty search results`(repositoryUrl: String): Unit = runBlocking {
        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(false)

        // When
        val result = uut.scanForNewKMPArtifacts(repositoryUrl).toList()

        // Then
        assertEquals(0, result.size)
        verify(iteratorSearchResponse).close()
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should handle exceptions during scanning`(repositoryUrl: String): Unit = runBlocking {
        val failure = RuntimeException("Search failed")
        whenever(indexer.searchIterator(any())).thenThrow(failure)

        // When & Then
        val thrown = assertFailsWith<RuntimeException> {
            uut.scanForNewKMPArtifacts(repositoryUrl).toList()
        }
        assertSame(failure, thrown)
        verifyNoInteractions(iteratorSearchResponse, iteratorResultSet)
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

        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(true, true, false)
        whenever(iteratorResultSet.next()).thenReturn(artifactInfo)

        val artifacts = uut.scanForNewKMPArtifacts(repositoryUrl)
        verifyNoInteractions(indexer, iteratorSearchResponse, iteratorResultSet)

        val result = artifacts.take(1).toList()

        assertEquals("first-artifact", result.single().artifactId)
        verify(iteratorResultSet).next()
        verify(iteratorSearchResponse).close()
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should close search results and context when iteration fails`(repositoryUrl: String): Unit = runBlocking {
        val failure = IllegalStateException("Iteration failed")
        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(true)
        whenever(iteratorResultSet.next()).thenThrow(failure)

        val thrown = assertFailsWith<IllegalStateException> {
            uut.scanForNewKMPArtifacts(repositoryUrl).toList()
        }

        assertSame(failure, thrown)
        verify(iteratorSearchResponse).close()
        verifyIndexingContext(repositoryUrl)
    }

    private fun verifyIndexingContext(repositoryUrl: String) {
        val indexDir = File(tempDir, "maven-central-index")
        verify(indexer).createIndexingContext(
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
        verify(indexer).closeIndexingContext(indexingContext, false)
    }

    companion object {
        private const val CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/"
        private const val GOOGLE_MIRROR_REPOSITORY_URL = "https://maven-central.storage-download.googleapis.com/maven2/"
    }
}
