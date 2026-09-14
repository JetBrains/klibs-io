package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.app.api.ProcessPackageIndexRequestsRequest
import io.klibs.app.api.ProcessPackageIndexRequestsRequest.PackageCoordinates
import io.klibs.app.api.ProcessPackageIndexRequestsResponse
import io.klibs.app.enums.ProcessPackageIndexAction
import io.klibs.app.enums.ProcessResultStatus
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.MavenArtifactRepository
import io.klibs.core.pckg.repository.NonKmpPackageRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.core.pckg.repository.UserRequestReportRepository
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.Base64

@ActiveProfiles("test")
class PackageIndexRequestControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var mavenArtifactRepository: MavenArtifactRepository

    @Autowired
    private lateinit var nonKmpPackageRepository: NonKmpPackageRepository

    @Autowired
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @Autowired
    private lateinit var userRequestReportRepository: UserRequestReportRepository

    @Test
    fun `test remove action with specific version`() {
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "remove-lib",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REMOVE,
            packages = listOf(PackageCoordinates("org.example", "remove-lib", "1.0.0")),
            reason = "Test removal",
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        assertEquals(1, response.results.size)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.SUCCESS, result.status)
        assertEquals(ProcessPackageIndexAction.REMOVE, result.action)
        assertEquals(1, result.affectedRequestsCount)

        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "remove-lib").isEmpty())
    }

    @Test
    fun `test remove action without version removes all matching requests`() {
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "multi-lib",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "multi-lib",
                version = "2.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REMOVE,
            packages = listOf(PackageCoordinates("org.example", "multi-lib")),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.SUCCESS, result.status)
        assertEquals(2, result.affectedRequestsCount)
        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "multi-lib").isEmpty())
    }

    @Test
    fun `test remove action returns not found when request does not exist`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REMOVE,
            packages = listOf(PackageCoordinates("org.nonexistent", "not-found", "1.0.0")),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.NOT_FOUND, result.status)
        assertEquals(0, result.affectedRequestsCount)
    }

    @Test
    fun `test mark non-kmp action with explicit version removes request and records non-kmp package`() {
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "non-kmp-lib",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.example", "non-kmp-lib", "1.0.0")),
            errorType = PackageIndexingErrorType.MISSING_TOOLING_METADATA,
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.SUCCESS, result.status)
        assertEquals(ProcessPackageIndexAction.MARK_NON_KMP, result.action)
        assertEquals(1, result.affectedRequestsCount)

        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "non-kmp-lib").isEmpty())
        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "non-kmp-lib", "1.0.0")
        assertNotNull(artifact)
        assertTrue(nonKmpPackageRepository.existsByMavenArtifactId(requireNotNull(artifact?.id)))
    }

    @Test
    fun `test mark non-kmp action without version records non-kmp for all versions in queue`() {
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "non-kmp-multi",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "non-kmp-multi",
                version = "1.1.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.example", "non-kmp-multi")),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.SUCCESS, result.status)
        assertEquals(2, result.affectedRequestsCount)

        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "non-kmp-multi").isEmpty())
        val artifact1 = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "non-kmp-multi", "1.0.0")
        val artifact2 = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "non-kmp-multi", "1.1.0")
        assertNotNull(artifact1)
        assertNotNull(artifact2)
        assertTrue(nonKmpPackageRepository.existsByMavenArtifactId(requireNotNull(artifact1?.id)))
        assertTrue(nonKmpPackageRepository.existsByMavenArtifactId(requireNotNull(artifact2?.id)))
    }

    @Test
    fun `test mark non-kmp action with explicit version not in queue succeeds`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.example", "external-non-kmp", "3.0.0")),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.SUCCESS, result.status)
        assertEquals(0, result.affectedRequestsCount)

        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "external-non-kmp", "3.0.0")
        assertNotNull(artifact)
        assertTrue(nonKmpPackageRepository.existsByMavenArtifactId(requireNotNull(artifact?.id)))
    }

    @Test
    fun `test mark non-kmp action without version and not in queue returns error`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.example", "missing-non-kmp")),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        val result = response.results[0]
        assertEquals(ProcessResultStatus.ERROR, result.status)
        assertEquals(0, result.affectedRequestsCount)
        assertTrue(result.message?.contains("Version is required") == true)
    }

    @Test
    fun `test mark non-kmp action is idempotent when repeated`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.example", "idempotent-pkg", "1.0.0")),
        )

        // First call
        mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }

        // Second call
        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(1, response.totalProcessed)
        assertEquals(ProcessResultStatus.SUCCESS, response.results[0].status)
    }

    @Test
    fun `test batch processing with multiple coordinates and actions`() {
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.batch",
                artifactId = "item-one",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )
        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.batch",
                artifactId = "item-two",
                version = "2.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.SEARCH_MAVEN,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REMOVE,
            packages = listOf(
                PackageCoordinates("org.batch", "item-one", "1.0.0"),
                PackageCoordinates("org.batch", "item-two", "2.0.0"),
                PackageCoordinates("org.batch", "item-three", "3.0.0"),
            ),
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(3, response.totalProcessed)
        assertEquals(ProcessResultStatus.SUCCESS, response.results[0].status)
        assertEquals(1, response.results[0].affectedRequestsCount)
        assertEquals(ProcessResultStatus.SUCCESS, response.results[1].status)
        assertEquals(1, response.results[1].affectedRequestsCount)
        assertEquals(ProcessResultStatus.NOT_FOUND, response.results[2].status)
        assertEquals(0, response.results[2].affectedRequestsCount)
    }

    @Test
    fun `test user request issue failure report generation on manual processing`() {
        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = 999,
                groupId = "org.user",
                artifactId = "reported-lib",
                version = "1.0.0",
            )
        )

        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.user",
                artifactId = "reported-lib",
                version = "1.0.0",
                releasedAt = Instant.now(),
                repo = ScraperType.CENTRAL_SONATYPE,
                userRequestIssue = issue,
            )
        )

        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            packages = listOf(PackageCoordinates("org.user", "reported-lib", "1.0.0")),
            reason = "Rejected: not KMP compatible",
        )

        val response = mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

        assertEquals(ProcessResultStatus.SUCCESS, response.results[0].status)
        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.user", "reported-lib").isEmpty())

        val reports = userRequestReportRepository.findAll().toList()
        val failureReport = reports.firstOrNull { it.userRequestIssue.githubIssueNumber == 999 }
        assertNotNull(failureReport)
        assertEquals(UserRequestIndexingStatus.FAILURE, failureReport?.indexingStatus)
        assertEquals("Rejected: not KMP compatible", failureReport?.statusDetails)
    }
}
