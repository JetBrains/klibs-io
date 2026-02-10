package io.klibs.integration.maven.index

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexingContextManager
import kotlinx.coroutines.runBlocking
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexingContext
import org.apache.maven.index.updater.IndexUpdateResult
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.File
import java.time.Instant
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
    private lateinit var indexingContextManager: MavenIndexingContextManager

    @Mock
    private lateinit var restClientBuilder: RestClient.Builder

    @Mock
    private lateinit var restClient: RestClient

    @Mock
    private lateinit var requestHeadersUriSpec: RestClient.RequestHeadersUriSpec<*>

    @Mock
    private lateinit var requestHeadersSpec: RestClient.RequestHeadersSpec<*>

    @Mock
    private lateinit var responseSpec: RestClient.ResponseSpec

    @Mock
    private lateinit var properties: MavenIntegrationProperties

    @Mock
    private lateinit var centralProperties: MavenIntegrationProperties.Central

    private lateinit var service: MavenIndexDownloadingService

    @BeforeEach
    fun setup() {
        whenever(properties.central).thenReturn(centralProperties)
        whenever(centralProperties.indexEndpoint).thenReturn("http://example.com")
        whenever(restClientBuilder.baseUrl(any<String>())).thenReturn(restClientBuilder)
        whenever(restClientBuilder.build()).thenReturn(restClient)

        service = MavenIndexDownloadingService(
            indexUpdater,
            resourceFetcher,
            indexingContextManager,
            properties,
            restClientBuilder
        )
    }

    @Test
    @DisplayName("Indexer should be always fully downloaded, because incremental updates are broken and could not contain all the libraries.")
    fun `should download full index with force flag`() {
        runBlocking {
            whenever(indexingContextManager.useCentralContext<Any>(any(), any())).thenAnswer { invocation ->
                val block = invocation.getArgument<suspend (IndexingContext) -> Any>(1)
                runBlocking { block(indexingContext) }
            }

            // Mock fetchRemoteIndexTimestamp
            val props = "nexus.index.timestamp=20260130185500.000 +0000"
            whenever(restClient.get()).thenReturn(requestHeadersUriSpec as RestClient.RequestHeadersUriSpec<Nothing>)
            whenever(requestHeadersUriSpec.uri(any<String>())).thenReturn(requestHeadersSpec as RestClient.RequestHeadersSpec<Nothing>)
            whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
            whenever(responseSpec.body<String>()).thenReturn(props)

            whenever(indexUpdateResult.isFullUpdate).thenReturn(true)
            whenever(indexUpdater.fetchAndUpdateIndex(any())).thenReturn(indexUpdateResult)

            service.downloadIndexIfNewer(Instant.EPOCH)

            verify(indexUpdater).fetchAndUpdateIndex(check {
                assertTrue(it.isForceFullUpdate, "Should have forceFullUpdate flag set to true")
            })
        }
    }
}
