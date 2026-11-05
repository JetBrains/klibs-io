package io.klibs.app.indexing

import io.klibs.app.configuration.properties.IndexingConfigurationProperties
import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.MavenCoordinateService
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.service.RejectedMavenCoordinatesService
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.service.MavenStaticDataProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.transaction.support.TransactionTemplate

class PackageIndexingServiceTestOld {
    private val discoverer = mockk<PackageDiscoverer>()
    private val providers: List<MavenStaticDataProvider> = listOf(
        provider(ScraperType.CENTRAL_SONATYPE),
        provider(ScraperType.GOOGLE_MAVEN),
        provider(ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR)
    )
    private val gitHubIndexingService = mockk<GitHubIndexingService>()
    private val projectIndexingService = mockk<ProjectIndexingService>()
    private val pomIndexingService = mockk<PomIndexingService>()
    private val kotlinToolingMetadataIndexingService = mockk<KotlinToolingMetadataIndexingService>()
    private val packageDescriptionGenerator = mockk<PackageDescriptionGenerator>()
    private val indexingRequestRepository = mockk<IndexingRequestRepository>()
    private val userRequestReportWriter = mockk<UserRequestReportWriter>()
    private val packageService = mockk<PackageService>()
    private val packageRepository = mockk<PackageRepository>()
    private val mavenCoordinateService = mockk<MavenCoordinateService>()
    private val rejectedMavenCoordinatesService = mockk<RejectedMavenCoordinatesService>()
    private val transactionTemplate = mockk<TransactionTemplate>()
    private val selfProvider = mockk<ObjectProvider<PackageIndexingService>>()

    private lateinit var service: PackageIndexingService

    @BeforeEach
    fun setup() {
        every { transactionTemplate.execute<Any?>(any()) } returns null

        service = PackageIndexingService(
            listOf(discoverer),
            providers,
            gitHubIndexingService,
            projectIndexingService,
            pomIndexingService,
            kotlinToolingMetadataIndexingService,
            packageDescriptionGenerator,
            indexingRequestRepository,
            userRequestReportWriter,
            packageService,
            packageRepository,
            mockk(),
            mavenCoordinateService,
            mockk<PackageIndexingErrorHandler>(),
            IndexingConfigurationProperties(),
            selfProvider
        )
    }

    @Test
    fun `should discover and queue new packages`() = runTest {
        val artifact = MavenArtifact(
            groupId = "org.jetbrains.kotlin",
            artifactId = "kotlin-stdlib",
            version = "1.9.0",
            scraperType = ScraperType.SEARCH_MAVEN,
        )

        coEvery { discoverer.discover(any()) } answers {
            val channel = firstArg<Channel<Exception>>()
            channel.close()
            flowOf(artifact)
        }

        every { indexingRequestRepository.saveAll(any<Iterable<IndexingRequestEntity>>()) } returns listOf(IndexingRequestEntity(
            id = 1L,
            groupId = artifact.groupId,
            artifactId = artifact.artifactId,
            version = artifact.version,
            repo = artifact.scraperType
        ))

        service.indexNewPackages()

        verify(exactly = 1) {
            indexingRequestRepository.saveAll(
                match<Iterable<IndexingRequestEntity>> { requests ->
                    val list = requests.toList()
                    list.size == 1 && list[0].groupId == artifact.groupId &&
                            list[0].artifactId == artifact.artifactId &&
                            list[0].version == artifact.version &&
                            list[0].repo == artifact.scraperType
                }
            )
        }
    }

    private fun provider(scraperType: ScraperType): MavenStaticDataProvider =
        mockk<MavenStaticDataProvider>().also { provider ->
            every { provider.scraperType } returns scraperType
        }
}
