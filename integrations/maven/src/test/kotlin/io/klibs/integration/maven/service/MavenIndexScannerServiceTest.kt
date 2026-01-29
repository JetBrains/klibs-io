package io.klibs.integration.maven.service

import io.klibs.integration.maven.ScraperType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.maven.index.*
import org.apache.maven.index.context.IndexingContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class MavenIndexScannerServiceTest {

    @Mock
    private lateinit var indexer: Indexer

    @Mock
    private lateinit var indexingContextManager: MavenIndexingContextManager

    @Mock
    private lateinit var indexingContext: IndexingContext

    @Mock
    private lateinit var iteratorSearchResponse: IteratorSearchResponse

    @Mock
    private lateinit var iteratorResultSet: IteratorResultSet

    private lateinit var uut: MavenIndexScannerService

    @BeforeEach
    fun setup() {
        uut = MavenIndexScannerService(indexer, indexingContextManager)
    }

    @Test
    fun `should scan and emit MavenArtifact objects`(): Unit = runBlocking {
        // Given
        val from = Instant.parse("2023-01-01T00:00:00Z")
        val to = Instant.parse("2023-01-02T00:00:00Z")

        whenever(indexingContextManager.useCentralContext<Any>(any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend (IndexingContext) -> Any>(1)
            runBlocking { block(indexingContext) }
        }

        val artifactInfo = ArtifactInfo().apply {
            groupId = "io.klibs"
            artifactId = "test-artifact"
            version = "1.0.0"
            lastModified = from.toEpochMilli()
        }

        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(true, false)
        whenever(iteratorResultSet.next()).thenReturn(artifactInfo)

        // When
        val result = uut.scanForKotlinToolingMetadata(from, to).toList()

        // Then
        assertEquals(1, result.size)
        val artifact = result[0]
        assertEquals("io.klibs", artifact.groupId)
        assertEquals("test-artifact", artifact.artifactId)
        assertEquals("1.0.0", artifact.version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, artifact.scraperType)
        assertEquals(from, artifact.releasedAt)

        verify(indexer).searchIterator(any())
    }

    @Test
    fun `should handle empty search results`(): Unit = runBlocking {
        // Given
        val from = Instant.parse("2023-01-01T00:00:00Z")
        val to = Instant.parse("2023-01-02T00:00:00Z")

        whenever(indexingContextManager.useCentralContext<Any>(any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend (IndexingContext) -> Any>(1)
            runBlocking { block(indexingContext) }
        }

        whenever(indexer.searchIterator(any())).thenReturn(iteratorSearchResponse)
        whenever(iteratorSearchResponse.iterator()).thenReturn(iteratorResultSet)
        whenever(iteratorResultSet.hasNext()).thenReturn(false)

        // When
        val result = uut.scanForKotlinToolingMetadata(from, to).toList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `should handle exceptions during scanning`(): Unit = runBlocking {
        // Given
        val from = Instant.parse("2023-01-01T00:00:00Z")
        val to = Instant.parse("2023-01-02T00:00:00Z")

        whenever(indexingContextManager.useCentralContext<Any>(any(), any())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend (IndexingContext) -> Any>(1)
            runBlocking { block(indexingContext) }
        }

        whenever(indexer.searchIterator(any())).thenThrow(RuntimeException("Search failed"))

        // When & Then
        assertFailsWith<RuntimeException> {
            uut.scanForKotlinToolingMetadata(from, to).toList()
        }
    }
}
