package io.klibs.integration.maven.index

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.configuration.MavenIndexerConfiguration
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexingContextManager
import io.klibs.integration.maven.service.impl.BaseMavenCentralStaticDataProvider
import io.klibs.integration.maven.service.impl.GoogleMavenCentralMirrorStaticDataProvider
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import kotlinx.coroutines.runBlocking
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexingContext
import org.apache.maven.index.updater.IndexUpdateResult
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class MavenIndexDownloadingServiceTest {

    @TempDir
    lateinit var tempDir: File

    @Mock
    private lateinit var indexer: Indexer

    @Mock
    private lateinit var indexUpdater: IndexUpdater

    @Mock
    private lateinit var resourceFetcher: ResourceFetcher

    @Mock
    private lateinit var indexingContext: IndexingContext

    @Mock
    private lateinit var indexUpdateResult: IndexUpdateResult

    @Mock
    private lateinit var provider: BaseMavenCentralStaticDataProvider

    private lateinit var indexingContextManager: MavenIndexingContextManager
    private lateinit var properties: MavenIntegrationProperties
    private lateinit var service: MavenIndexDownloadingService

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
        service = MavenIndexDownloadingService(
            indexUpdater,
            resourceFetcher,
            indexingContextManager,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    @DisplayName(
        "Indexer should be always fully downloaded, " +
            "because incremental updates are broken and could not contain all the libraries."
    )
    fun `should download full index with force flag`(repositoryUrl: String): Unit = runBlocking {
        stubProviderContext(repositoryUrl)
        whenever(provider.fetchRemoteIndexTimestamp()).thenReturn(INDEX_TIMESTAMP)
        val localIndexCacheDir = File(tempDir, "local-index-cache")

        whenever(indexUpdateResult.isFullUpdate).thenReturn(true)
        whenever(indexUpdater.fetchAndUpdateIndex(any())).thenReturn(indexUpdateResult)

        val result = service.downloadIndexIfNewer(Instant.EPOCH, provider)

        assertEquals(INDEX_TIMESTAMP, result)
        verify(indexUpdater).fetchAndUpdateIndex(check {
            assertTrue(it.isForceFullUpdate, "Should have forceFullUpdate flag set to true")
            assertSame(indexingContext, it.indexingContext)
            assertSame(resourceFetcher, it.resourceFetcher)
            assertEquals(File(tempDir, "tmp"), it.indexTempDir, "Should use the configured temp directory")
            assertEquals(
                localIndexCacheDir,
                it.localIndexCacheDir,
                "Should use local index cache dir to decouple download from extraction",
            )
        })
        verify(provider).fetchRemoteIndexTimestamp()
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest(name = "{0}: remote timestamp {1}")
    @MethodSource("unchangedTimestamps")
    fun `should not update when timestamp is missing or not newer`(
        repositoryUrl: String,
        remoteIndexTimestamp: Instant?,
    ): Unit = runBlocking {
        stubProviderContext(repositoryUrl)
        whenever(provider.fetchRemoteIndexTimestamp()).thenReturn(remoteIndexTimestamp)

        assertNull(service.downloadIndexIfNewer(INDEX_TIMESTAMP, provider))

        verify(provider).fetchRemoteIndexTimestamp()
        verifyNoInteractions(indexUpdater, resourceFetcher)
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should propagate timestamp failure and close context`(repositoryUrl: String): Unit = runBlocking {
        stubProviderContext(repositoryUrl)
        val failure = IllegalStateException("Timestamp lookup failed")
        whenever(provider.fetchRemoteIndexTimestamp()).thenThrow(failure)

        val thrown = assertFailsWith<IllegalStateException> {
            service.downloadIndexIfNewer(Instant.EPOCH, provider)
        }

        assertSame(failure, thrown)
        verify(provider).fetchRemoteIndexTimestamp()
        verifyNoInteractions(indexUpdater, resourceFetcher)
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should propagate download failure and close context`(repositoryUrl: String): Unit = runBlocking {
        stubProviderContext(repositoryUrl)
        whenever(provider.fetchRemoteIndexTimestamp()).thenReturn(INDEX_TIMESTAMP)
        val failure = IOException("Index download failed")
        whenever(indexUpdater.fetchAndUpdateIndex(any())).thenThrow(failure)

        val thrown = assertFailsWith<IOException> {
            service.downloadIndexIfNewer(Instant.EPOCH, provider)
        }

        assertSame(failure, thrown)
        verify(provider).fetchRemoteIndexTimestamp()
        verify(indexUpdater).fetchAndUpdateIndex(any())
        verifyIndexingContext(repositoryUrl)
    }

    @ParameterizedTest
    @ValueSource(strings = [CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL])
    fun `should request properties and archive from selected repository`(repositoryUrl: String): Unit = runBlocking {
        val indexProperties = """
            nexus.index.id=central
            nexus.index.timestamp=20260130185500.000 +0000
            nexus.index.chain-id=20260130185500
        """.trimIndent()
        val transport = mock<Transport>()
        val response = mock<Transport.Response>()
        whenever(response.code).thenReturn(200)
        whenever(response.body).thenAnswer { indexProperties.byteInputStream() }
        whenever(transport.get(any(), any())).thenReturn(response)
        val selectedProvider = createProvider(repositoryUrl, transport)
        assertEquals(repositoryUrl, selectedProvider.contentEndpoint)

        val archiveFailure = IOException("Stop before downloading the index archive")
        val requestedUrls = mutableListOf<String>()
        var indexUrl: String? = null
        var disconnected = false
        val recordingFetcher = object : ResourceFetcher {
            override fun connect(id: String?, url: String?) {
                indexUrl = requireNotNull(url)
            }

            override fun disconnect() {
                disconnected = true
            }

            override fun retrieve(name: String?): InputStream {
                requestedUrls += "${checkNotNull(indexUrl)}/${requireNotNull(name)}"
                return when (name) {
                    "nexus-maven-repository-index.properties" -> indexProperties.byteInputStream()
                    "nexus-maven-repository-index.gz", "nexus-maven-repository-index.zip" -> throw archiveFailure
                    else -> error("Unexpected index resource: $name")
                }
            }
        }
        val configuration = MavenIndexerConfiguration()
        val realIndexer = spy(configuration.indexer())
        val contextManager = MavenIndexingContextManager(properties, realIndexer, configuration.indexCreators())
        service = MavenIndexDownloadingService(configuration.indexUpdater(), recordingFetcher, contextManager)

        val thrown = assertFailsWith<IOException> {
            service.downloadIndexIfNewer(Instant.EPOCH, selectedProvider)
        }

        assertSame(archiveFailure, thrown)
        val expectedIndexUrl = "${repositoryUrl.trimEnd('/')}/.index"
        verify(transport).get("$expectedIndexUrl/nexus-maven-repository-index.properties", emptyMap())
        verify(response).close()
        assertEquals(
            listOf(
                "$expectedIndexUrl/nexus-maven-repository-index.properties",
                "$expectedIndexUrl/nexus-maven-repository-index.gz",
                "$expectedIndexUrl/nexus-maven-repository-index.zip",
            ),
            requestedUrls,
        )
        assertTrue(disconnected)
        verify(realIndexer).closeIndexingContext(any(), eq(false))
    }

    private fun stubProviderContext(repositoryUrl: String) {
        whenever(provider.contentEndpoint).thenReturn(repositoryUrl)
        whenever(indexer.createIndexingContext(any(), any(), any(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(indexingContext)
    }

    private fun verifyIndexingContext(repositoryUrl: String) {
        val indexDir = File(tempDir, "maven-central-index")
        verify(indexer).createIndexingContext(
            "maven-central-context",
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

    private fun createProvider(repositoryUrl: String, transport: Transport): BaseMavenCentralStaticDataProvider {
        val rateLimiter = mock<MavenCentralRateLimiter>()
        whenever(rateLimiter.withRateLimitBlocking(any<() -> Any?>())).thenAnswer { invocation ->
            invocation.getArgument<() -> Any?>(0).invoke()
        }
        return if (repositoryUrl == GOOGLE_MIRROR_REPOSITORY_URL) {
            GoogleMavenCentralMirrorStaticDataProvider(
                xmlMapper = XmlMapper(),
                mavenCentralRateLimiter = rateLimiter,
                objectMapper = ObjectMapper(),
                contentEndpoint = repositoryUrl,
                clientTransport = transport,
            )
        } else {
            SonatypeCentralStaticDataProvider(
                xmlMapper = XmlMapper(),
                mavenCentralRateLimiter = rateLimiter,
                objectMapper = ObjectMapper(),
                contentEndpoint = repositoryUrl,
                clientTransport = transport,
            )
        }
    }

    companion object {
        private const val CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/"
        private const val GOOGLE_MIRROR_REPOSITORY_URL = "https://maven-central.storage-download.googleapis.com/maven2/"
        private val INDEX_TIMESTAMP = Instant.parse("2026-01-30T18:55:00Z")

        @JvmStatic
        fun unchangedTimestamps(): List<Arguments> = listOf(CENTRAL_REPOSITORY_URL, GOOGLE_MIRROR_REPOSITORY_URL)
            .flatMap { repositoryUrl ->
                listOf(INDEX_TIMESTAMP, INDEX_TIMESTAMP.minusSeconds(1), null).map { timestamp ->
                    Arguments.of(repositoryUrl, timestamp)
                }
            }
    }
}
