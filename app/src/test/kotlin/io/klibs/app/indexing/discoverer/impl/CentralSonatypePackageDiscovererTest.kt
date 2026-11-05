package io.klibs.app.indexing.discoverer.impl

import io.klibs.core.pckg.dto.projection.Package
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.BlacklistRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.service.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import io.klibs.integration.maven.service.impl.BaseMavenCentralStaticDataProvider
import io.klibs.integration.maven.service.impl.GoogleMavenCentralMirrorStaticDataProvider
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest(classes = [CentralSonatypePackageDiscoverer::class])
@ActiveProfiles("test")
internal class CentralSonatypePackageDiscovererTest {

    @MockkBean
    lateinit var mavenIndexDownloadingService: MavenIndexDownloadingService

    @MockkBean(name = "centralSonatypeMavenIndexScannerService")
    lateinit var mavenIndexScannerService: MavenIndexScannerService

    @MockkBean(name = "centralSonatypeScraper")
    lateinit var centralSonatypeScraper: MavenCentralScraper

    @MockkBean(name = "googleMavenCentralMirrorIndexScannerService")
    lateinit var googleMavenCentralMirrorIndexScannerService: MavenIndexScannerService

    @MockkBean(name = "googleMavenCentralMirrorScraper")
    lateinit var googleMavenCentralMirrorScraper: MavenCentralScraper

    @MockkBean(relaxUnitFun = true)
    lateinit var mavenCentralLogRepository: MavenCentralLogRepository

    @MockkBean
    lateinit var packageRepository: PackageRepository

    @MockkBean(relaxed = true)
    lateinit var blacklistRepository: BlacklistRepository

    @MockkBean
    lateinit var centralSonatypeStaticDataProvider: SonatypeCentralStaticDataProvider

    @MockkBean
    lateinit var googleMavenCentralMirrorStaticDataProvider: GoogleMavenCentralMirrorStaticDataProvider

    lateinit var discoverer: CentralSonatypePackageDiscoverer

    private val initialTimestamp = Instant.parse("2023-01-01T00:00:00Z")
    private val originUrl = "https://repo1.maven.org/maven2"
    private val mirrorUrl = "https://maven-central.storage-download.googleapis.com/maven2"

    @BeforeEach
    fun setUp() {
        every { mavenCentralLogRepository.retrieveMavenIndexTimestamp() } returns initialTimestamp
        every { centralSonatypeStaticDataProvider.contentEndpoint } returns originUrl
        every { centralSonatypeStaticDataProvider.scraperType } returns ScraperType.CENTRAL_SONATYPE
        every { googleMavenCentralMirrorStaticDataProvider.contentEndpoint } returns mirrorUrl
        every { googleMavenCentralMirrorStaticDataProvider.scraperType } returns
            ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR

        discoverer = CentralSonatypePackageDiscoverer(
            mavenIndexDownloadingService,
            mavenIndexScannerService,
            centralSonatypeScraper,
            mavenCentralLogRepository,
            packageRepository,
            centralSonatypeStaticDataProvider,
            blacklistRepository,
        )
    }

    private fun discovererFor(mirror: Boolean): BaseMavenCentralPackageDiscoverer = if (mirror) {
        GoogleMavenCentralMirrorPackageDiscoverer(
            mavenIndexDownloadingService,
            googleMavenCentralMirrorIndexScannerService,
            googleMavenCentralMirrorScraper,
            mavenCentralLogRepository,
            packageRepository,
            googleMavenCentralMirrorStaticDataProvider,
            blacklistRepository,
        )
    } else discoverer


    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should exclude banned packages from full discovery`(mirror: Boolean) = runTest {
        every { packageRepository.findAllKnownMavenCentralPackages() } returns emptyList()
        every { blacklistRepository.checkPackageBanned("com.example", "lib") } returns true
        every { blacklistRepository.checkPackageBanned("com.banned", "anything") } returns true
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns initialTimestamp
        val source = providerFor(mirror).scraperType
        val allowed = MavenArtifact("com.example", "sibling", "1.0", source)
        val contentEndpoint = providerFor(mirror).contentEndpoint
        every { scannerFor(mirror).scanForNewKMPArtifacts(contentEndpoint) } returns flowOf(
            MavenArtifact("com.example", "lib", "1.0", source),
            MavenArtifact("com.banned", "anything", "1.0", source),
            allowed,
        )

        assertEquals(listOf(allowed), discovererFor(mirror).discover(Channel()).toList())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should exclude banned coordinates before requesting new versions`(mirror: Boolean) = runTest {
        every { packageRepository.findAllKnownMavenCentralPackages() } returns listOf(
            Package("com.example", "lib", setOf("1.0")),
            Package("com.banned", "anything", setOf("1.0")),
            Package("com.example", "sibling", setOf("1.0")),
        )
        every { blacklistRepository.checkPackageBanned("com.example", "lib") } returns true
        every { blacklistRepository.checkPackageBanned("com.banned", "anything") } returns true
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns null
        coEvery { scraperFor(mirror).findNewVersions(any(), any()) } returns flowOf()

        discovererFor(mirror).discover(Channel()).toList()

        coVerify(exactly = 1) { scraperFor(mirror).findNewVersions(
            match<Map<String, Set<String>>> { it == mapOf("com.example:sibling" to setOf("1.0")) }, any()
        ) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should discover new versions for known packages in UPDATE_KNOWN mode`(mirror: Boolean) = runTest {
        // Given
        val knownPackage = Package(
            groupId = "org.example",
            artifactId = "test-lib",
            versions = setOf("1.0.0")
        )

        val newVersion = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "1.1.0",
            scraperType = providerFor(mirror).scraperType,
        )

        every { packageRepository.findAllKnownMavenCentralPackages() } returns listOf(knownPackage)
        coEvery { scraperFor(mirror).findNewVersions(any(), any()) } returns flowOf(newVersion)
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns null

        val errorChannel = Channel<Exception>()

        // When
        val artifacts = discovererFor(mirror).discover(errorChannel).toList()

        // Then
        assertEquals(1, artifacts.size)
        assertEquals("1.1.0", artifacts[0].version)
        assertEquals("test-lib", artifacts[0].artifactId)
        assertEquals(providerFor(mirror).scraperType, artifacts[0].scraperType)

        coVerify(exactly = 1) { mavenIndexDownloadingService.downloadIndexIfNewer(initialTimestamp, providerFor(mirror)) }
        verify(exactly = 0) { scannerFor(mirror).scanForNewKMPArtifacts(any()) }
        coVerify(exactly = 1) { scraperFor(mirror).findNewVersions(
            match<Map<String, Set<String>>> { it.containsKey("org.example:test-lib") },
            any()
        ) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should discover new maven artifacts`(mirror: Boolean) = runTest {
        // Given
        val artifact1 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib1",
            version = "1.0.0",
            scraperType = providerFor(mirror).scraperType,
        )

        val artifact2 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib2",
            version = "1.0.0",
            scraperType = providerFor(mirror).scraperType,
        )

        every { packageRepository.findAllKnownMavenCentralPackages() } returns emptyList()
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns initialTimestamp
        val contentEndpoint = providerFor(mirror).contentEndpoint
        every { scannerFor(mirror).scanForNewKMPArtifacts(contentEndpoint) } returns
                flowOf(artifact1, artifact2)

        val errorChannel = Channel<Exception>()

        val artifacts = discovererFor(mirror).discover(errorChannel = errorChannel).toList()

        assertEquals(2, artifacts.size)

        val resultArtifact1 = artifacts.find { it.artifactId == "test-lib1" }
        assertEquals("org.example", resultArtifact1?.groupId)
        assertEquals("test-lib1", resultArtifact1?.artifactId)
        assertEquals("1.0.0", resultArtifact1?.version)
        assertEquals(providerFor(mirror).scraperType, resultArtifact1?.scraperType)

        val resultArtifact2 = artifacts.find { it.artifactId == "test-lib2" }
        assertEquals("org.example", resultArtifact2?.groupId)
        assertEquals("test-lib2", resultArtifact2?.artifactId)
        assertEquals("1.0.0", resultArtifact2?.version)
        assertEquals(providerFor(mirror).scraperType, resultArtifact2?.scraperType)

        coVerify(exactly = 1) { mavenIndexDownloadingService.downloadIndexIfNewer(initialTimestamp, providerFor(mirror)) }
        verify(exactly = 1) { scannerFor(mirror).scanForNewKMPArtifacts(contentEndpoint) }
        verify(exactly = 0) { scannerFor(!mirror).scanForNewKMPArtifacts(any()) }
        verify(exactly = 1) { mavenCentralLogRepository.saveMavenIndexTimestamp(any()) }
    }

    @Test
    fun `should filter out known artifacts`() = runTest {
        // Given
        val knownArtifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "known-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
        )

        val newArtifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "new-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
        )

        val knownPackage = Package(
            groupId = "org.example",
            artifactId = "known-lib",
            versions = setOf("1.0.0")
        )

        every { packageRepository.findAllKnownMavenCentralPackages() } returns listOf(knownPackage)
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns initialTimestamp
        every { mavenIndexScannerService.scanForNewKMPArtifacts(originUrl) } returns
            flowOf(knownArtifact, newArtifact)

        val errorChannel = Channel<Exception>()

        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(1, artifacts.size)
        assertEquals("new-lib", artifacts[0].artifactId)

        // Verify the timestamp was updated with the latest artifact timestamp
        verify(exactly = 1) { mavenCentralLogRepository.saveMavenIndexTimestamp(any()) }
    }

    @Test
    fun `should find all versions for artifacts`() = runTest {
        // Given
        val artifact1 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
        )

        val artifact2 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "2.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
        )

        every { packageRepository.findAllKnownMavenCentralPackages() } returns emptyList()
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns initialTimestamp
        every { mavenIndexScannerService.scanForNewKMPArtifacts(originUrl) } returns flowOf(artifact1, artifact2)

        val errorChannel = Channel<Exception>()

        // When
        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(2, artifacts.size)
        val versions = artifacts.map { it.version }.sorted()
        assertEquals(listOf("1.0.0", "2.0.0"), versions)

        verify(exactly = 1) { mavenCentralLogRepository.saveMavenIndexTimestamp(any()) }
    }

    @Test
    fun `should handle empty results`() = runTest {
        every { packageRepository.findAllKnownMavenCentralPackages() } returns emptyList()
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(any(), any()) } returns initialTimestamp
        every { mavenIndexScannerService.scanForNewKMPArtifacts(originUrl) } returns flowOf()

        val errorChannel = Channel<Exception>()

        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(0, artifacts.size)
        verify(exactly = 1) { mavenCentralLogRepository.saveMavenIndexTimestamp(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should report download errors and update known packages using the same source`(mirror: Boolean) = runTest {
        val failure = IllegalStateException("Index download failed")
        val errorChannel = Channel<Exception>(Channel.UNLIMITED)
        val artifact = MavenArtifact("org.example", "test-lib", "1.1.0", providerFor(mirror).scraperType)
        every { packageRepository.findAllKnownMavenCentralPackages() } returns
            listOf(Package("org.example", "test-lib", setOf("1.0.0")))
        coEvery { mavenIndexDownloadingService.downloadIndexIfNewer(initialTimestamp, providerFor(mirror)) } throws
            failure
        coEvery { scraperFor(mirror).findNewVersions(any(), any()) } returns flowOf(artifact)

        assertEquals(listOf(artifact), discovererFor(mirror).discover(errorChannel).toList())
        val error = errorChannel.receive()
        assertEquals("Failed to download Maven index", error.message)
        assertEquals(failure, error.cause)
        verify(exactly = 0) { scannerFor(mirror).scanForNewKMPArtifacts(any()) }
        verify(exactly = 0) { mavenCentralLogRepository.saveMavenIndexTimestamp(any()) }
    }

    private fun providerFor(mirror: Boolean): BaseMavenCentralStaticDataProvider =
        if (mirror) googleMavenCentralMirrorStaticDataProvider else centralSonatypeStaticDataProvider

    private fun scannerFor(mirror: Boolean): MavenIndexScannerService =
        if (mirror) googleMavenCentralMirrorIndexScannerService else mavenIndexScannerService

    private fun scraperFor(mirror: Boolean): MavenCentralScraper =
        if (mirror) googleMavenCentralMirrorScraper else centralSonatypeScraper


}