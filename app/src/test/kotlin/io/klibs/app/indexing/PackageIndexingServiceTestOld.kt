package io.klibs.app.indexing

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.ScraperType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

class PackageIndexingServiceTestOld {
    private val discoverer: PackageDiscoverer = mock()
    private val providers: Map<String, MavenStaticDataProvider> = mapOf(
        "maven_central" to mock(),
        "gmaven" to mock(),
        "gcloud" to mock()
    )
    private val gitHubIndexingService: GitHubIndexingService = mock()
    private val projectIndexingService: ProjectIndexingService = mock()
    private val packageDescriptionGenerator: PackageDescriptionGenerator = mock()
    private val indexingRequestRepository: IndexingRequestRepository = mock()
    private val packageService: PackageService = mock()
    private val packageRepository: PackageRepository = mock()
    private val transactionTemplate: TransactionTemplate = mock()

    private lateinit var service: PackageIndexingService

    @BeforeEach
    fun setup() {
        whenever(transactionTemplate.execute<Any?>(any())).thenReturn(null)

        service = PackageIndexingService(
            listOf(discoverer),
            providers,
            gitHubIndexingService,
            projectIndexingService,
            packageDescriptionGenerator,
            indexingRequestRepository,
            packageService,
            packageRepository
        )
    }

    @Test
    fun `should discover and queue new packages`() = runTest {
        val artifact = MavenArtifact(
            groupId = "org.jetbrains.kotlin",
            artifactId = "kotlin-stdlib",
            version = "1.9.0",
            scraperType = ScraperType.SEARCH_MAVEN,
            releasedAt = Instant.now()
        )

        whenever(discoverer.discover(any())).thenAnswer { invocation ->
            val channel = invocation.getArgument<Channel<Exception>>(0)
            channel.close()
            flowOf(artifact)
        }

        whenever(indexingRequestRepository.saveAll(any<Iterable<IndexingRequestEntity>>())).thenReturn(listOf(IndexingRequestEntity(
            id = 1L,
            groupId = artifact.groupId,
            artifactId = artifact.artifactId,
            version = artifact.version,
            releasedAt = artifact.releasedAt,
            repo = artifact.scraperType
        )))
        whenever(indexingRequestRepository.removeRepeating()).thenReturn(0)

        service.indexNewPackages()

        verify(indexingRequestRepository).saveAll(argThat<Iterable<IndexingRequestEntity>> { requests ->
            val list = requests.toList()
            list.size == 1 && list[0].groupId == artifact.groupId &&
                    list[0].artifactId == artifact.artifactId &&
                    list[0].version == artifact.version &&
                    list[0].repo == artifact.scraperType
        })
        verify(indexingRequestRepository).removeRepeating()
    }
}
