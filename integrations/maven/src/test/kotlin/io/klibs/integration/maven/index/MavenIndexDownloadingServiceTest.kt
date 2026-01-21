package io.klibs.integration.maven.index

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.service.MavenIndexDownloadingService
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever
import java.io.File
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

    private val properties = MavenIntegrationProperties(
        central = MavenIntegrationProperties.Central(
            rateLimitCapacity = 100,
            rateLimitRefillAmount = 10,
            rateLimitRefillPeriodSec = 1,
            discoveryEndpoint = "",
            indexEndpoint = "",
            indexDir = ""
        )
    )

    private lateinit var service: MavenIndexDownloadingService

    @BeforeEach
    fun setup() {
        service = MavenIndexDownloadingService(
            properties.copy(central = properties.central.copy(indexDir = File(tempDir, "maven-index").absolutePath)),
            indexer,
            indexUpdater,
            emptyList(),
            resourceFetcher
        )
    }

    @Test
    @DisplayName("Indexer should be always fully downloaded, because incremental updates are broken and could not contain all the libraries.")
    fun `should download full index with force flag`() {
        whenever(
            indexer.createIndexingContext(any(), any(), any(), any(), any(), anyOrNull(), any(), any(), any())
        ).thenReturn(indexingContext)

        whenever(indexUpdateResult.isFullUpdate).thenReturn(true)
        whenever(indexUpdater.fetchAndUpdateIndex(any())).thenReturn(indexUpdateResult)

        service.downloadFullIndex()

        verify(indexUpdater).fetchAndUpdateIndex(check {
            assertTrue(it.isForceFullUpdate, "Should have forceFullUpdate flag set to true")
        })
    }
}
