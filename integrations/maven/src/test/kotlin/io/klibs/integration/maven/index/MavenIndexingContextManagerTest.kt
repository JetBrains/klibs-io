package io.klibs.integration.maven.index

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.configuration.MavenIndexerConfiguration
import io.klibs.integration.maven.service.MavenIndexingContextManager
import kotlinx.coroutines.runBlocking
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.context.IndexingContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MavenIndexingContextManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var indexer: Indexer
    private lateinit var indexCreators: List<IndexCreator>
    private lateinit var uut: MavenIndexingContextManager

    @BeforeEach
    fun setup() {
        val configuration = MavenIndexerConfiguration()
        indexer = spy(configuration.indexer())
        indexCreators = configuration.indexCreators()
        val properties = MavenIntegrationProperties(
            central = MavenIntegrationProperties.Central(
                rateLimitCapacity = 100,
                rateLimitRefillAmount = 10,
                rateLimitRefillPeriodSec = 1,
                indexDir = tempDir.absolutePath,
            )
        )
        uut = MavenIndexingContextManager(properties, indexer, indexCreators)
    }

    @ParameterizedTest
    @MethodSource("repositoryUrls")
    fun `should derive index URL and preserve directories and block result`(repositoryUrl: String): Unit = runBlocking {
        val expectedResult = Any()
        val indexDir = File(tempDir, "maven-central-index")
        lateinit var indexingContext: IndexingContext

        val result = uut.useCentralContext("test-context", repositoryUrl) { context ->
            indexingContext = context
            assertEquals(repositoryUrl, context.repositoryUrl)
            assertEquals("${repositoryUrl.trimEnd('/')}/.index", context.indexUpdateUrl)
            assertEquals(indexDir, context.indexDirectoryFile)
            assertTrue(indexDir.isDirectory)
            assertEquals(File(tempDir, "tmp"), uut.getIndexTmpDir())
            assertEquals(File(tempDir, "local-index-cache"), uut.getLocalIndexCacheDir())
            expectedResult
        }

        assertSame(expectedResult, result)
        verify(indexer).createIndexingContext(
            "test-context",
            "central",
            indexDir,
            indexDir,
            repositoryUrl,
            null,
            true,
            true,
            indexCreators,
        )
        verify(indexer).closeIndexingContext(indexingContext, false)
        assertTrue(indexDir.isDirectory, "Closing the context should preserve the local index")
    }

    @ParameterizedTest
    @MethodSource("repositoryUrls")
    fun `should propagate block failure and close context without deleting index`(repositoryUrl: String): Unit =
        runBlocking {
            val failure = IllegalStateException("Index processing failed")
            lateinit var indexingContext: IndexingContext

            val thrown = assertFailsWith<IllegalStateException> {
                uut.useCentralContext<Unit>("failing-context", repositoryUrl) { context ->
                    indexingContext = context
                    throw failure
                }
            }

            assertSame(failure, thrown)
            verify(indexer).closeIndexingContext(indexingContext, false)
            assertTrue(File(tempDir, "maven-central-index").isDirectory)
        }

    companion object {
        private const val CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/"
        private const val GOOGLE_MIRROR_REPOSITORY_URL = "https://maven-central.storage-download.googleapis.com/maven2/"

        @JvmStatic
        fun repositoryUrls(): List<String> = listOf(CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL)
            .flatMap { listOf(it, it.trimEnd('/')) }
    }
}