package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.util.instant.InstantRepository
import io.klibs.core.pckg.dto.projection.Package
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.scraper.MavenCentralScraper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import org.springframework.boot.test.context.SpringBootTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [CentralSonatypePackageDiscoverer::class])
@ActiveProfiles("test")
internal class CentralSonatypePackageDiscovererTest {

    @MockkBean
    lateinit var centralSonatypeScraper: MavenCentralScraper

    @MockkBean
    lateinit var lastPackageIndexedRepository: InstantRepository

    @MockkBean
    lateinit var packageRepository: PackageRepository

    lateinit var discoverer: CentralSonatypePackageDiscoverer

    private val initialTimestamp = Instant.parse("2023-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        every { lastPackageIndexedRepository.retrieveLatest() } returns initialTimestamp
        discoverer = CentralSonatypePackageDiscoverer(
            centralSonatypeScraper,
            lastPackageIndexedRepository,
            packageRepository,
            3
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

        every { packageRepository.findAllKnownPackages() } returns emptyList()
        val expectedStartTs = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs, errorChannel) } returns flowOf(artifact1, artifact2)
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(artifact1, errorChannel) } returns flowOf(artifact1)
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(artifact2, errorChannel) } returns flowOf(artifact2)

        val savedTs = slot<java.time.Instant>()
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

        verify(exactly = 1) { lastPackageIndexedRepository.save(capture(savedTs)) }
        assertTrue(savedTs.captured.isAfter(initialTimestamp))
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

        every { packageRepository.findAllKnownPackages() } returns listOf(knownPackage)
        val expectedStartTs2 = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel2 = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs2, errorChannel2) } returns flowOf(knownArtifact, newArtifact)
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(newArtifact, errorChannel2) } returns flowOf(newArtifact)

        val savedTs2 = slot<java.time.Instant>()
        val artifacts = discoverer.discover(errorChannel = errorChannel2).toList()

        assertEquals(1, artifacts.size)
        assertEquals("new-lib", artifacts[0].artifactId)

        // Verify the timestamp was updated
        verify(exactly = 1) { lastPackageIndexedRepository.save(capture(savedTs2)) }
        assertTrue(savedTs2.captured.isAfter(initialTimestamp))
    }

    @Test
    fun `should find all versions for artifacts`() = runTest {
        // Given
        val artifact = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )

        val version2 = MavenArtifact(
            groupId = "org.example",
            artifactId = "test-lib",
            version = "2.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(7200)
        )

        every { packageRepository.findAllKnownPackages() } returns emptyList()
        val expectedStartTs3 = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel3 = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs3, errorChannel3) } returns flowOf(artifact)
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(artifact, errorChannel3) } returns (
            flowOf(
                artifact,
                version2
            )
        )

        // When
        val savedTs3 = slot<java.time.Instant>()
        val artifacts = discoverer.discover(errorChannel = errorChannel3).toList()

        assertEquals(2, artifacts.size)
        val versions = artifacts.map { it.version }.sorted()
        assertEquals(listOf("1.0.0", "2.0.0"), versions)

        verify(exactly = 1) { lastPackageIndexedRepository.save(capture(savedTs3)) }
        assertTrue(savedTs3.captured.isAfter(initialTimestamp))
    }

    @Test
    fun `should handle empty results`() = runTest {
        every { packageRepository.findAllKnownPackages() } returns emptyList()
        val expectedStartTs4 = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel4 = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs4, errorChannel4) } returns flowOf()

        val savedTs4 = slot<java.time.Instant>()
        val artifacts = discoverer.discover(errorChannel = errorChannel4).toList()

        assertEquals(0, artifacts.size)
        verify(exactly = 1) { lastPackageIndexedRepository.save(capture(savedTs4)) }
        assertTrue(savedTs4.captured.isAfter(initialTimestamp))
    }

    /**
     * Problem that is covered is that:
     *  1. findKmpArtifacts can emit 2 versions of the same artifact
     *  2. for each version, if findAllVersionForArtifact will be triggered -- that will cause flow to have duplicate versions
     *  3. when we try to insert them later, we will hit unique groupId-artifactId-version constraint on package_index_request table
     */
    @Test
    fun `should deduplicate equal artifacts from findKmpArtifacts`() = runTest {
        val dup1 = MavenArtifact(
            groupId = "org.example",
            artifactId = "dup-lib",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )
        val dup2 = dup1.copy()

        every { packageRepository.findAllKnownPackages() } returns emptyList()
        val expectedStartTs5 = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel5 = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs5, errorChannel5) } returns flowOf(dup1, dup2)
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(dup1, errorChannel5) } returns flowOf(dup1, dup2)

        // shouldn't be called
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(dup2, errorChannel5) } returns flowOf(dup1, dup2)

        val artifacts = discoverer.discover(errorChannel = errorChannel5).toList()

        assertEquals(1, artifacts.size)

        coVerify(exactly = 1) { centralSonatypeScraper.findAllVersionForArtifact(dup1, errorChannel5) }
    }

    @Test
    fun `should deduplicate equal artifacts from findAllVersionForArtifact`() = runTest {
        val base = MavenArtifact(
            groupId = "org.example",
            artifactId = "dup-lib-versions",
            version = "1.0.0",
            scraperType = ScraperType.CENTRAL_SONATYPE,
            releasedAt = initialTimestamp.plusSeconds(3600)
        )
        val duplicate = base.copy()

        every { packageRepository.findAllKnownPackages() } returns emptyList()
        val expectedStartTs6 = initialTimestamp.minusSeconds(3 * 3600L)
        val errorChannel6 = Channel<Exception>()
        coEvery { centralSonatypeScraper.findKmpArtifacts(expectedStartTs6, errorChannel6) } returns flowOf(base)

        // findAllVersionForArtifact mistakenly returns duplicates
        coEvery { centralSonatypeScraper.findAllVersionForArtifact(base, errorChannel6) } returns flowOf(base, duplicate)

        val artifacts = discoverer.discover(errorChannel = errorChannel6).toList()

        assertEquals(1, artifacts.size)

        coVerify(exactly = 1) { centralSonatypeScraper.findAllVersionForArtifact(base, errorChannel6) }
    }
}