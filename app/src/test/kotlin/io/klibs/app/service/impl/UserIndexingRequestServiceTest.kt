package io.klibs.app.service.impl

import BaseUnitWithDbLayerTest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import io.klibs.integration.maven.dto.MavenMetadata
import io.klibs.integration.maven.search.impl.BaseCentralMavenSearchClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

class UserIndexingRequestServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: BaseUserIndexingRequestService

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Autowired
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @MockitoBean
    private lateinit var centralSonatypeSearchClient: BaseCentralMavenSearchClient

    // Tests for specific artifact with given version

    @Test
    fun `should throw 400 when artifact is not a KMP library`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(null)

        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest()
        }

        assertEquals(
            "Artifact com.example:lib:1.0.0 is not a valid Kotlin Multiplatform library (kotlin-tooling-metadata.json not found)",
            exception.reason
        )
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-package.sql"])
    fun `should throw 400 when a specific artifact is already indexed`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest()
        }

        assertEquals("Artifact com.example:lib:1.0.0 is already indexed or queued", exception.reason)
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-package-index-request.sql"])
    fun `should throw 400 when a specific artifact is already in package_index_request`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest()
        }

        assertEquals("Artifact com.example:lib:1.0.0 is already indexed or queued", exception.reason)
    }

    @Test
    fun `should save index request for valid specific version`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        fulfillRequest()

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        assertTrue(saved != null, "Index request should be saved")
        assertEquals("com.example", saved.groupId, "Wrong groupId")
        assertEquals("lib", saved.artifactId, "Wrong artifactId")
        assertEquals("1.0.0", saved.version, "Wrong version")
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved.repo, "Wrong scraper type")
    }

    @Test
    @Transactional
    fun `should link saved index requests to the originating user request issue`() {
        whenever(centralSonatypeSearchClient.getKotlinToolingMetadata(any())).thenReturn(mock<KotlinToolingMetadataDelegateStubImpl>())

        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = 42,
                groupId = "com.example",
                artifactId = "lib",
                version = "1.0.0",
            )
        )

        uut.fulfillRequest(requireNotNull(issue.id))

        val saved = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        assertTrue(saved != null, "Index request should be saved")
        assertEquals(issue.id, saved.userRequestIssue?.id, "Index request should be linked to the originating issue")
    }

    // Tests when no specific version is provided

    @Test
    fun `should throw 400 when no KMP artifacts found for group and artifactId`() {
        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest(version = null)
        }

        assertEquals("No Kotlin Multiplatform artifacts found for com.example:lib", exception.reason)
    }

    @Test
    fun `should save index requests when search returns artifacts`() {
        whenever(centralSonatypeSearchClient.getMavenMetadata(eq("com.example"), eq("lib")))
            .thenReturn(
                MavenMetadata(
                    "com.example",
                    "lib",
                    MavenMetadata.Versioning(versions = listOf("1.0.0", "2.0.0", "3.0.0"))
                )
            )
        fulfillRequest(version = null)

        val saved1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        val saved2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "2.0.0")
        val saved3 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "3.0.0")
        assertTrue(saved1 != null, "First artifact should be saved")
        assertTrue(saved2 != null, "Second artifact should be saved")
        assertTrue(saved3 != null, "Third artifact should be saved")
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved1.repo)
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved2.repo)
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved3.repo)
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-package.sql"])
    fun `should save index request for multiple artifacts that aren't indexed yet`() {
        whenever(centralSonatypeSearchClient.getMavenMetadata(eq("com.example"), eq("lib")))
            .thenReturn(
                MavenMetadata(
                    "com.example",
                    "lib",
                    MavenMetadata.Versioning(versions = listOf("1.0.0", "2.0.0", "3.0.0"))
                )
            )

        fulfillRequest(version = null)

        val old = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "1.0.0")
        val saved1 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "2.0.0")
        val saved2 = indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion("com.example", "lib", "3.0.0")
        assertEquals(old, null, "Indexed artifact shouldn't be saved")
        assertTrue(saved1 != null, "Second artifact should be saved")
        assertTrue(saved2 != null, "Third artifact should be saved")
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved1.repo)
        assertEquals(ScraperType.CENTRAL_SONATYPE, saved2.repo)
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-package.sql"])
    fun `should throw 400 when all artifacts are already indexed`() {
        whenever(centralSonatypeSearchClient.getMavenMetadata(eq("com.example"), eq("libA")))
            .thenReturn(
                MavenMetadata(
                    "com.example",
                    "libA",
                    MavenMetadata.Versioning(versions = listOf("1.0.0", "2.0.0"))
                )
            )


        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest(artifactId = "libA", version = null)
        }

        assertEquals("All artifacts from this request are already indexed, queued or banned", exception.reason)
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-banned-packages.sql"])
    fun `should throw 400 when a specific artifact is banned`() {
        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest()
        }

        assertEquals("Artifact com.example:lib:1.0.0 is banned", exception.reason)
    }

    @Test
    @Sql(value = ["classpath:/sql/UserIndexingRequestServiceTest/insert-into-banned-packages.sql"])
    fun `should throw 400 when all discovered artifacts are banned by group`() {
        whenever(centralSonatypeSearchClient.getMavenMetadata(eq("com.banned"), eq("libX")))
            .thenReturn(
                MavenMetadata(
                    "com.banned",
                    "libX",
                    MavenMetadata.Versioning(versions = listOf("1.0.0", "2.0.0"))
                )
            )
        val exception = assertThrows<UserRequestProcessingException> {
            fulfillRequest(groupId = "com.banned", artifactId = "libX", version = null)
        }

        assertEquals("All artifacts from this request are already indexed, queued or banned", exception.reason)
    }

    private fun fulfillRequest(
        groupId: String = "com.example",
        artifactId: String = "lib",
        version: String? = "1.0.0",
        githubIssueNumber: Int = 42,
    ) {
        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = githubIssueNumber,
                groupId = groupId,
                artifactId = artifactId,
                version = version,
            )
        )

        uut.fulfillRequest(requireNotNull(issue.id))
    }
}