package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import tools.jackson.databind.ObjectMapper
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
import io.klibs.core.pckg.repository.MavenCoordinateRepository
import io.klibs.core.pckg.repository.RejectedMavenCoordinateRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.core.pckg.repository.UserRequestReportRepository
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@ActiveProfiles("test")
class PackageIndexRequestControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var mavenArtifactRepository: MavenCoordinateRepository

    @Autowired
    private lateinit var rejectedMavenCoordinateRepository: RejectedMavenCoordinateRepository

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
            action = ProcessPackageIndexAction.REJECT,
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
        assertEquals(ProcessPackageIndexAction.REJECT, result.action)
        assertEquals(1, result.affectedRequestsCount)

        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "non-kmp-lib").isEmpty())
        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "non-kmp-lib", "1.0.0")
        assertNotNull(artifact)
        assertTrue(rejectedMavenCoordinateRepository.existsByMavenCoordinateId(requireNotNull(artifact?.id)))
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
            action = ProcessPackageIndexAction.REJECT,
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
        assertTrue(rejectedMavenCoordinateRepository.existsByMavenCoordinateId(requireNotNull(artifact1?.id)))
        assertTrue(rejectedMavenCoordinateRepository.existsByMavenCoordinateId(requireNotNull(artifact2?.id)))
    }

    @Test
    fun `test mark reject action with explicit version not in queue succeeds`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REJECT,
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
        assertEquals(ProcessResultStatus.ERROR, result.status)
        assertEquals(0, result.affectedRequestsCount)

        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "external-non-kmp", "3.0.0")
        assertNull(artifact)
        assertEquals(0L, rejectedMavenCoordinateRepository.count())
        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", "external-non-kmp").isEmpty())
    }

    @Test
    fun `test mark non-kmp action without version and not in queue returns error`() {
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REJECT,
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
    fun `test mark reject action is idempotent when repeated`() {
        enqueue("idempotent-pkg", repo = ScraperType.GOOGLE_MAVEN)
        val payload = ProcessPackageIndexRequestsRequest(
            action = ProcessPackageIndexAction.REJECT,
            packages = listOf(PackageCoordinates("org.example", "idempotent-pkg", "1.0.0")),
        )

        val firstResponse = process(payload)
        assertEquals(ProcessResultStatus.SUCCESS, firstResponse.results.single().status)
        assertEquals(1, firstResponse.results.single().affectedRequestsCount)
        assertRejected("idempotent-pkg", ScraperType.GOOGLE_MAVEN)
        val response = process(payload)

        assertEquals(1, response.totalProcessed)
        assertEquals(ProcessResultStatus.SUCCESS, response.results[0].status)
        assertEquals(0, response.results[0].affectedRequestsCount)
        assertEquals(1L, rejectedMavenCoordinateRepository.count())
        assertRejected("idempotent-pkg", ScraperType.GOOGLE_MAVEN)
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
            action = ProcessPackageIndexAction.REJECT,
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

    @Test
    fun `test rejection without error type leaves queue unchanged`() {
        val queued = enqueue("missing-error-type")
        val response = process(
            ProcessPackageIndexRequestsRequest(
                action = ProcessPackageIndexAction.REJECT,
                packages = listOf(PackageCoordinates("org.example", "missing-error-type", "1.0.0")),
                errorType = null,
            )
        )

        assertEquals(ProcessResultStatus.ERROR, response.results.single().status)
        assertEquals(0, response.results.single().affectedRequestsCount)
        assertTrue(indexingRequestRepository.existsById(requireNotNull(queued.id)))
        assertNoRejection("missing-error-type")
    }

    @Test
    fun `test rejection with mixed explicit and versionless queue entries changes nothing`() {
        for (version in listOf("1.0.0", null)) {
            val artifactId = if (version == null) "mixed-all" else "mixed-explicit"
            val explicit = enqueue(artifactId)
            val versionless = enqueue(artifactId, version = null)
            val response = process(
                ProcessPackageIndexRequestsRequest(
                    action = ProcessPackageIndexAction.REJECT,
                    packages = listOf(PackageCoordinates("org.example", artifactId, version)),
                )
            )

            assertEquals(ProcessResultStatus.ERROR, response.results.single().status)
            assertEquals(0, response.results.single().affectedRequestsCount)
            assertEquals(
                setOf(explicit.id, versionless.id),
                indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", artifactId).map { it.id }.toSet(),
            )
            assertNoRejection(artifactId)
        }
    }

    @Test
    fun `test reject alias records rejection and removes queued request`() {
        enqueue("reject-alias", repo = ScraperType.GOOGLE_MAVEN)
        val response = process(
            ProcessPackageIndexRequestsRequest(
                action = ProcessPackageIndexAction.REJECT,
                packages = listOf(PackageCoordinates("org.example", "reject-alias", "1.0.0")),
                errorType = PackageIndexingErrorType.MISSING_TOOLING_METADATA,
            )
        )

        assertEquals(ProcessResultStatus.SUCCESS, response.results.single().status)
        assertEquals(ProcessPackageIndexAction.REJECT, response.results.single().action)
        assertEquals(1, response.results.single().affectedRequestsCount)
        assertRejected("reject-alias", ScraperType.GOOGLE_MAVEN)
    }

    @Test
    fun `test mixed rejection batch preserves successes around missing source`() {
        enqueue("valid-before")
        enqueue("valid-after")
        val response = process(
            ProcessPackageIndexRequestsRequest(
                action = ProcessPackageIndexAction.REJECT,
                packages = listOf("valid-before", "missing-source", "valid-after").map {
                    PackageCoordinates("org.example", it, "1.0.0")
                },
            )
        )

        assertEquals(3, response.totalProcessed)
        assertEquals(
            listOf(ProcessResultStatus.SUCCESS, ProcessResultStatus.ERROR, ProcessResultStatus.SUCCESS),
            response.results.map { it.status },
        )
        assertEquals(listOf(1, 0, 1), response.results.map { it.affectedRequestsCount })
        assertRejected("valid-before")
        assertRejected("valid-after")
        assertNull(mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "missing-source", "1.0.0"))
        assertEquals(2L, rejectedMavenCoordinateRepository.count())
    }

    @Test
    fun `test database report failure rolls back coordinate but preserves other batch successes`() {
        val issue = userRequestIssueRepository.save(
            UserRequestIssueEntity(
                githubIssueNumber = 1000,
                groupId = "org.example",
                artifactId = "rollback-report",
                version = "1.0.0",
            )
        )
        val queued = enqueue("rollback-report", issue = issue)
        enqueue("rollback-before")
        enqueue("rollback-after")
        val response = process(
            ProcessPackageIndexRequestsRequest(
                action = ProcessPackageIndexAction.REJECT,
                packages = listOf("rollback-before", "rollback-report", "rollback-after").map {
                    PackageCoordinates("org.example", it, "1.0.0")
                },
                // PostgreSQL text columns cannot store a NUL character.
                reason = "Invalid report\u0000reason",
            )
        )

        assertEquals(3, response.totalProcessed)
        assertEquals(
            listOf(ProcessResultStatus.SUCCESS, ProcessResultStatus.ERROR, ProcessResultStatus.SUCCESS),
            response.results.map { it.status },
        )
        assertEquals(listOf(1, 0, 1), response.results.map { it.affectedRequestsCount })
        assertTrue(indexingRequestRepository.existsById(requireNotNull(queued.id)))
        assertTrue(userRequestIssueRepository.existsById(requireNotNull(issue.id)))
        assertNull(mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", "rollback-report", "1.0.0"))
        assertTrue(userRequestReportRepository.findAll().none { it.artifactId == "rollback-report" })
        assertRejected("rollback-before")
        assertRejected("rollback-after")
        assertEquals(2L, rejectedMavenCoordinateRepository.count())
    }

    private fun enqueue(
        artifactId: String,
        version: String? = "1.0.0",
        repo: ScraperType = ScraperType.SEARCH_MAVEN,
        issue: UserRequestIssueEntity? = null,
    ): IndexingRequestEntity = indexingRequestRepository.save(
        IndexingRequestEntity(
            groupId = "org.example",
            artifactId = artifactId,
            version = version,
            releasedAt = Instant.now(),
            repo = repo,
            userRequestIssue = issue,
        )
    )

    private fun process(payload: ProcessPackageIndexRequestsRequest): ProcessPackageIndexRequestsResponse =
        mockMvc.post("/package-index-request/manual/process") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isOk() }
        }.andReturn().let {
            objectMapper.readValue(it.response.contentAsString, ProcessPackageIndexRequestsResponse::class.java)
        }

    private fun assertRejected(artifactId: String, repo: ScraperType = ScraperType.SEARCH_MAVEN) {
        val coordinate = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", artifactId, "1.0.0")
        assertNotNull(coordinate)
        val rejection = rejectedMavenCoordinateRepository.findAll().filter { it.mavenCoordinate.id == coordinate?.id }.single()
        assertEquals(PackageIndexingErrorType.MISSING_TOOLING_METADATA, rejection.errorType)
        assertEquals(repo, rejection.repo)
        assertTrue(indexingRequestRepository.findAllByGroupIdAndArtifactId("org.example", artifactId).isEmpty())
    }

    private fun assertNoRejection(artifactId: String) {
        assertNull(mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion("org.example", artifactId, "1.0.0"))
        assertEquals(0L, rejectedMavenCoordinateRepository.count())
    }
}
