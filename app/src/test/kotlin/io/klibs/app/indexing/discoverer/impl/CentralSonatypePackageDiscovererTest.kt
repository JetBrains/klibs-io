package io.klibs.app.indexing.discoverer.impl

import io.klibs.core.pckg.dto.projection.Package
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.project.blacklist.BlacklistRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.service.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [CentralSonatypePackageDiscoverer::class])
@ActiveProfiles("test")
internal class CentralSonatypePackageDiscovererTest {

    @MockitoBean
    lateinit var mavenIndexDownloadingService: MavenIndexDownloadingService

    @MockitoBean(name = "centralSonatypeMavenIndexScannerService")
    lateinit var mavenIndexScannerService: MavenIndexScannerService

    @MockitoBean(name = "centralSonatypeScraper")
    lateinit var centralSonatypeScraper: MavenCentralScraper

    @MockitoBean
    lateinit var mavenCentralLogRepository: MavenCentralLogRepository

    @MockitoBean
    lateinit var packageRepository: PackageRepository

    @MockitoBean
    lateinit var blacklistRepository: BlacklistRepository

    @MockitoBean
    lateinit var centralSonatypeStaticDataProvider: SonatypeCentralStaticDataProvider

    lateinit var discoverer: CentralSonatypePackageDiscoverer

    private val initialTimestamp = Instant.parse("2023-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        whenever(mavenCentralLogRepository.retrieveMavenIndexTimestamp()).thenReturn(initialTimestamp)

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
            mavenIndexScannerService,
            centralSonatypeScraper,
            mavenCentralLogRepository,
            packageRepository,
            centralSonatypeStaticDataProvider,
            blacklistRepository,
        )
    } else discoverer

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should exclude banned packages from full discovery`(mirror: Boolean) = runTest {
        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(emptyList())
        whenever(blacklistRepository.checkPackageBanned("com.example", "lib")).thenReturn(true)
        whenever(blacklistRepository.checkPackageBanned("com.banned", "anything")).thenReturn(true)
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(initialTimestamp)
        val allowed = MavenArtifact("com.example", "sibling", "1.0", ScraperType.CENTRAL_SONATYPE)
        whenever(mavenIndexScannerService.scanForNewKMPArtifacts()).thenReturn(flowOf(
            MavenArtifact("com.example", "lib", "1.0", ScraperType.CENTRAL_SONATYPE),
            MavenArtifact("com.banned", "anything", "1.0", ScraperType.CENTRAL_SONATYPE),
            allowed,
        ))

        assertEquals(listOf(allowed), discovererFor(mirror).discover(Channel()).toList())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should exclude banned coordinates before requesting new versions`(mirror: Boolean) = runTest {
        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(listOf(
            Package("com.example", "lib", setOf("1.0")),
            Package("com.banned", "anything", setOf("1.0")),
            Package("com.example", "sibling", setOf("1.0")),
        ))
        whenever(blacklistRepository.checkPackageBanned("com.example", "lib")).thenReturn(true)
        whenever(blacklistRepository.checkPackageBanned("com.banned", "anything")).thenReturn(true)
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(null)
        whenever(centralSonatypeScraper.findNewVersions(any(), any())).thenReturn(flowOf())

        discovererFor(mirror).discover(Channel()).toList()

        verify(centralSonatypeScraper).findNewVersions(
            argThat { this == mapOf("com.example:sibling" to setOf("1.0")) }, any()
        )
    }

    @Test
    fun `should discover new versions for known packages in UPDATE_KNOWN mode`() = runTest {
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
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = null
        )

        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(listOf(knownPackage))
        whenever(centralSonatypeScraper.findNewVersions(any(), any())).thenReturn(flowOf(newVersion))
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(null)

        val errorChannel = Channel<Exception>()

        // When
        val artifacts = discoverer.discover(errorChannel).toList()

        // Then
        assertEquals(1, artifacts.size)
        assertEquals("1.1.0", artifacts[0].version)
        assertEquals("test-lib", artifacts[0].artifactId)

        verify(centralSonatypeScraper).findNewVersions(
            argThat { containsKey("org.example:test-lib") },
            any()
        )
    }

    @Test
    fun `should discover new maven artifacts`() = runTest {
        // Given
        val artifact1 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib1",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )

        val artifact2 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib2",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(7200)
        )

        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(emptyList())
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(initialTimestamp)
        whenever(mavenIndexScannerService.scanForNewKMPArtifacts()).thenReturn(flowOf(artifact1, artifact2))

        val errorChannel = Channel<Exception>()

        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(2, artifacts.size)

        val resultArtifact1 = artifacts.find { it.artifactId == "test-lib1" }
        assertEquals("org.example", resultArtifact1?.groupId)
        assertEquals("test-lib1", resultArtifact1?.artifactId)
        assertEquals("1.0.0", resultArtifact1?.version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, resultArtifact1?.scraperType)
        assertTrue(resultArtifact1?.releasedAt != null)

        val resultArtifact2 = artifacts.find { it.artifactId == "test-lib2" }
        assertEquals("org.example", resultArtifact2?.groupId)
        assertEquals("test-lib2", resultArtifact2?.artifactId)
        assertEquals("1.0.0", resultArtifact2?.version)
        assertEquals(ScraperType.CENTRAL_SONATYPE, resultArtifact2?.scraperType)
        assertTrue(resultArtifact2?.releasedAt != null)

        verify(mavenCentralLogRepository, times(1)).saveMavenIndexTimestamp(any())
    }

    @Test
    fun `should filter out known artifacts`() = runTest {
        // Given
        val knownArtifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "known-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )

        val newArtifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "new-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(7200)
        )

        val knownPackage = Package(
            groupId = "org.example",
            artifactId = "known-lib",
            versions = setOf("1.0.0")
        )

        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(listOf(knownPackage))
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(initialTimestamp)
        whenever(mavenIndexScannerService.scanForNewKMPArtifacts()).thenReturn(flowOf(knownArtifact, newArtifact))

        val errorChannel = Channel<Exception>()

        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(1, artifacts.size)
        assertEquals("new-lib", artifacts[0].artifactId)

        // Verify the timestamp was updated with the latest artifact timestamp
        verify(mavenCentralLogRepository, times(1)).saveMavenIndexTimestamp(any())
    }

    @Test
    fun `should find all versions for artifacts`() = runTest {
        // Given
        val artifact1 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )

        val artifact2 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "2.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(7200)
        )

        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(emptyList())
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(initialTimestamp)
        whenever(mavenIndexScannerService.scanForNewKMPArtifacts()).thenReturn(flowOf(artifact1, artifact2))

        val errorChannel = Channel<Exception>()

        // When
        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(2, artifacts.size)
        val versions = artifacts.map { it.version }.sorted()
        assertEquals(listOf("1.0.0", "2.0.0"), versions)

        verify(mavenCentralLogRepository, times(1)).saveMavenIndexTimestamp(any())
    }

    @Test
    fun `should handle empty results`() = runTest {
        whenever(packageRepository.findAllKnownMavenCentralPackages()).thenReturn(emptyList())
        whenever(mavenIndexDownloadingService.downloadIndexIfNewer(any(), any())).thenReturn(initialTimestamp)
        whenever(mavenIndexScannerService.scanForNewKMPArtifacts()).thenReturn(flowOf())

        val errorChannel = Channel<Exception>()

        val artifacts = discoverer.discover(errorChannel = errorChannel).toList()

        assertEquals(0, artifacts.size)
        verify(mavenCentralLogRepository, times(1)).saveMavenIndexTimestamp(any())
    }

}