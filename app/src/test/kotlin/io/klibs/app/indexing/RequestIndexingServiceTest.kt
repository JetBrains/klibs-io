package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.MavenSearchResponse
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestIndexingServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: RequestIndexingService

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @MockitoBean
    private lateinit var centralSonatypeSearchClient: CentralSonatypeSearchClient

    // Tests for specific artifact with given version

    @Test
    fun `should throw 400 when artifact is not a KMP library`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(null)

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", "lib", "1.0.0")
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals(
            "Artifact com.example:lib:1.0.0 is not a valid Kotlin Multiplatform library (kotlin-tooling-metadata.json not found)",
            exception.reason!!
        )
    }

    @Test
    @Sql(value = ["classpath:sql/RequestIndexingServiceTest/insert-into-package.sql"])
    fun `should throw 400 when a specific artifact is already indexed`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", "lib", "1.0.0")
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("Artifact com.example:lib:1.0.0 is already indexed or queued", exception.reason!!)
    }

    @Test
    @Sql(value = ["classpath:sql/RequestIndexingServiceTest/insert-into-package-index-request.sql"])
    fun `should throw 400 when a specific artifact is already in package_index_request`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", "lib", "1.0.0")
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("Artifact com.example:lib:1.0.0 is already indexed or queued", exception.reason!!)
    }

    @Test
    fun `should save index request for valid specific version`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        uut.requestIndexing("com.example", "lib", "1.0.0")

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        assertTrue(saved != null, "Index request should be saved")
        assertEquals("com.example", saved.groupId, "Wrong groupId")
        assertEquals("lib", saved.artifactId, "Wrong artifactId")
        assertEquals("1.0.0", saved.version, "Wrong version")
        assertEquals(ScraperType.MANUAL_REQUEST, saved.repo, "Wrong scraper type")
    }

    // Tests when no specific version is provided

    @Test
    fun `should throw 503 when central sonatype search fails`() {
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenThrow(RuntimeException("Connection timeout"))

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", null, null)
        }

        assertEquals(503, exception.statusCode.value())
        assertEquals("Central Sonatype search failed", exception.reason!!)
    }

    @Test
    fun `should throw 400 when no KMP artifacts found for group`() {
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 0, currentHits = 0, page = emptyList()))

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", null, null)
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("No Kotlin Multiplatform artifacts found for com.example", exception.reason!!)
    }

    @Test
    fun `should throw 400 when no KMP artifacts found for group and artifactId`() {
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 0, currentHits = 0, page = emptyList()))

        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", "lib", null)
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("No Kotlin Multiplatform artifacts found for com.example:lib", exception.reason!!)
    }

    @Test
    fun `should save index requests when search returns artifacts`() {
        val artifacts = listOf(
            ArtifactData("com.example", "libA", "1.0.0"),
            ArtifactData("com.example", "libA", "2.0.0"),
            ArtifactData("com.example", "libB", "1.0.0"),
        )
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 3, page = artifacts))
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(1), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 0, page = emptyList()))

        uut.requestIndexing("com.example", null, null)

        val saved1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "1.0.0")
        val saved2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "2.0.0")
        val saved3 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libB", "1.0.0")
        assertTrue(saved1 != null, "First artifact should be saved")
        assertTrue(saved2 != null, "Second artifact should be saved")
        assertTrue(saved3 != null, "Third artifact should be saved")
        assertEquals(ScraperType.MANUAL_REQUEST, saved1.repo)
        assertEquals(ScraperType.MANUAL_REQUEST, saved2.repo)
        assertEquals(ScraperType.MANUAL_REQUEST, saved3.repo)
    }

    @Test
    @Sql(value = ["classpath:sql/RequestIndexingServiceTest/insert-into-package.sql"])
    fun `should save index request for multiple artifacts that aren't indexed yet`() {
        val artifacts = listOf(
            ArtifactData("com.example", "libA", "1.0.0"),
            ArtifactData("com.example", "libA", "2.0.0"),
            ArtifactData("com.example", "libB", "1.0.0"),
            ArtifactData("com.example", "libB", "2.0.0"),
            ArtifactData("com.example", "libC", "1.0.0"),
        )
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 5, currentHits = 5, page = artifacts))
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(1), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 5, currentHits = 0, page = emptyList()))


        uut.requestIndexing("com.example", null, null)

        val old1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "1.0.0")
        val old2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "2.0.0")
        val old3 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libB", "1.0.0")
        val saved1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libB", "2.0.0")
        val saved2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libC", "1.0.0")
        assertEquals(old1, null, "First artifact shouldn't be saved")
        assertEquals(old2, null, "Second artifact shouldn't be saved")
        assertEquals(old3, null, "Third artifact shouldn't be saved")
        assertTrue(saved1 != null, "Fourth artifact should be saved")
        assertTrue(saved2 != null, "Fifth artifact should be saved")
        assertEquals(ScraperType.MANUAL_REQUEST, saved1.repo)
        assertEquals(ScraperType.MANUAL_REQUEST, saved2.repo)
    }

    @Test
    @Sql(value = ["classpath:sql/RequestIndexingServiceTest/insert-into-package.sql"])
    fun `should throw 400 when all artifacts are already indexed`() {
        val artifacts = listOf(
            ArtifactData("com.example", "libA", "1.0.0"),
            ArtifactData("com.example", "libA", "2.0.0"),
            ArtifactData("com.example", "libB", "1.0.0"),
        )
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 3, page = artifacts))
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(1), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 0, page = emptyList()))


        val exception = assertThrows<ResponseStatusException> {
            uut.requestIndexing("com.example", null, null)
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals("All artifacts from this request are already indexed or queued", exception.reason!!)
    }

    @Test
    fun `should treat as if no version provided if artifactId is null`() {
        val artifacts = listOf(
            ArtifactData("com.example", "libA", "1.0.0"),
            ArtifactData("com.example", "libA", "2.0.0"),
            ArtifactData("com.example", "libB", "1.0.0"),
        )
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(0), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 3, page = artifacts))
        whenever(centralSonatypeSearchClient.searchWithThrottle(eq(1), any(), any()))
            .thenReturn(MavenSearchResponse(totalHits = 3, currentHits = 0, page = emptyList()))

        uut.requestIndexing("com.example", null, "1.0.0")

        val saved1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "1.0.0")
        val saved2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libA", "2.0.0")
        val saved3 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "libB", "1.0.0")
        assertTrue(saved1 != null, "First artifact should be saved")
        assertTrue(saved2 != null, "Second artifact should be saved")
        assertTrue(saved3 != null, "Third artifact should be saved")
        assertEquals(ScraperType.MANUAL_REQUEST, saved1.repo)
        assertEquals(ScraperType.MANUAL_REQUEST, saved2.repo)
        assertEquals(ScraperType.MANUAL_REQUEST, saved3.repo)
    }
}