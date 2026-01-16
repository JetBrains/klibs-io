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
import io.mockk.any
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

class PackageIndexingServiceTestOld {
    private val discoverer: PackageDiscoverer = mockk()
    private val providers: Map<String, MavenStaticDataProvider> = mapOf(
        "maven_central" to mockk(),
        "gmaven" to mockk(),
        "gcloud" to mockk()
    )
    private val gitHubIndexingService: GitHubIndexingService = mockk()
    private val projectIndexingService: ProjectIndexingService = mockk()
    private val packageDescriptionGenerator: PackageDescriptionGenerator = mockk()
    private val indexingRequestRepository: IndexingRequestRepository = mockk()
    private val packageService: PackageService = mockk()
    private val packageRepository: PackageRepository = mockk()
    private val transactionTemplate: TransactionTemplate = mockk()

    private lateinit var service: PackageIndexingService

    @BeforeEach
    fun setup() {
        every { transactionTemplate.execute<Any?>(any()) } returns null

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

        every { discoverer.discover(any()) } answers {
            val channel = firstArg<Channel<Exception>>()
            channel.close()
            flowOf(artifact)
        }

        every { indexingRequestRepository.saveAll(any<Iterable<IndexingRequestEntity>>()) } returns listOf(IndexingRequestEntity(
            id = 1L,
            groupId = artifact.groupId,
            artifactId = artifact.artifactId,
            version = artifact.version,
            releasedAt = artifact.releasedAt,
            repo = artifact.scraperType
        )))
        every { indexingRequestRepository.removeRepeating() } returns 0

        service.indexNewPackages()

        verify {
            indexingRequestRepository.saveAll(match<Iterable<IndexingRequestEntity>> { requests ->
                val list = requests.toList()
                list.size == 1 && list[0].groupId == artifact.groupId &&
                        list[0].artifactId == artifact.artifactId &&
                        list[0].version == artifact.version &&
                        list[0].repo == artifact.scraperType
            })
        }
        verify { indexingRequestRepository.removeRepeating() }
    }
}
