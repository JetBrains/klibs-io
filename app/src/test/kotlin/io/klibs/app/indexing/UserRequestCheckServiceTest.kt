package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class UserRequestCheckServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: UserRequestCheckService

    @Autowired
    private lateinit var mavenCentralLogRepository: MavenCentralLogRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockitoBean
    private lateinit var userRequestIndexingService: UserRequestIndexingService

    @Value("\${klibs.integration.github.index-requests.repository}")
    private lateinit var repoName: String

    @Value("\${klibs.integration.github.index-requests.request-label}")
    private lateinit var requestLabel: String

    @Value("\${klibs.integration.github.index-requests.processed-label}")
    private lateinit var processedLabel: String

    private fun body(g: String?, a: String?, v: String?) = buildString {
        if (g != null) append("### Group ID\n\n$g\n\n")
        if (a != null) append("### Artifact ID\n\n$a\n\n")
        if (v != null) append("### Version\n\n$v\n\n")
    }

    private fun defaultTimestamp() = LocalDate
        .now()
        .minusYears(10)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()

    private fun issue(
        number: Int,
        body: String?,
        labels: List<String> = listOf("index-request"),
    ) = GitHubIssue(
        number = number,
        title = "t",
        body = body,
        labels = labels,
        updatedAt = Instant.now(),
    )

    @Test
    fun `should comment, label as triaged and update timestamp on success`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, body("g", "a", null))))

        uut.checkUserRequests()

        verify(userRequestIndexingService).indexUserRequest("g", "a", null)
        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, processedLabel)

        verifyTimestampWasUpdated()
    }

    @Test
    fun `should update timestamp when no new issues were found`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(emptyList())

        uut.checkUserRequests()

        verify(userRequestIndexingService, never()).indexUserRequest(any(), any(), any())
        verify(gitHubIntegration, never()).addKlibsIssueComment(any(), any())
        verify(gitHubIntegration, never()).addKlibsIssueLabel(any(), any())

        verifyTimestampWasUpdated()
    }

    @Test
    fun `should comment with reason, label as triaged and update timestamp on user's error (4xx)`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, body("g", "a", null))))
        whenever(userRequestIndexingService.indexUserRequest("g", "a", null))
            .thenThrow(
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No Kotlin Multiplatform artifacts found for g.a"
                )
            )

        uut.checkUserRequests()

        verify(userRequestIndexingService).indexUserRequest("g", "a", null)
        verify(gitHubIntegration).addKlibsIssueComment(
            eq(123),
            argThat { contains("No Kotlin Multiplatform artifacts found for g.a") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, processedLabel)

        verifyTimestampWasUpdated()
    }

    @Test
    fun `should comment, label as triaged and update timestamp on incorrect issue body`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, "incorrect body")))

        uut.checkUserRequests()

        verify(userRequestIndexingService, never()).indexUserRequest(any(), any(), any())
        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("Could not read") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, processedLabel)

        verifyTimestampWasUpdated()
    }

    @Test
    fun `should comment with validation error, label as triaged and update timestamp on invalid request data`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, body("group with spaces", "a", "1.0.0"))))

        uut.checkUserRequests()

        verify(userRequestIndexingService, never()).indexUserRequest(any(), any(), any())
        verify(gitHubIntegration).addKlibsIssueComment(
            eq(123),
            argThat { contains("Invalid Group ID format") }
        )
        verify(gitHubIntegration).addKlibsIssueLabel(123, processedLabel)

        verifyTimestampWasUpdated()
    }

    @Test
    fun `should neither comment, label, nor update timestamp on server error (5xx)`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, body("g", "a", null))))
        whenever(userRequestIndexingService.indexUserRequest("g", "a", null))
            .thenThrow(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Central Sonatype search failed"))

        uut.checkUserRequests()

        verify(userRequestIndexingService).indexUserRequest("g", "a", null)
        verify(gitHubIntegration, never()).addKlibsIssueComment(any(), any())
        verify(gitHubIntegration, never()).addKlibsIssueLabel(any(), any())

        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())
    }

    @Test
    fun `should comment and label issues where possible and not update timestamp if server error (5xx) occurred for any`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(
                listOf(
                    issue(101, body("g1", "a1", null)),
                    issue(102, body("g2", "a2", null)),
                )
            )
        whenever(userRequestIndexingService.indexUserRequest("g1", "a1", null))
            .thenThrow(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Central Sonatype search failed"))


        uut.checkUserRequests()

        verify(gitHubIntegration, never()).addKlibsIssueComment(eq(101), any())
        verify(gitHubIntegration, never()).addKlibsIssueLabel(101, processedLabel)
        verify(gitHubIntegration).addKlibsIssueComment(eq(102), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(102, processedLabel)
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())
    }

    @Test
    fun `should exit early and not update timestamp when failed to list issues`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenThrow(RuntimeException("unexpected exception"))

        uut.checkUserRequests()

        verify(userRequestIndexingService, never()).indexUserRequest(any(), any(), any())

        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())
    }

    @Test
    fun `should treat unexpected exception during processing as server error`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(listOf(issue(123, body("g", "a", null))))
        whenever(userRequestIndexingService.indexUserRequest("g", "a", null))
            .thenThrow(IllegalStateException("unexpected exception"))

        uut.checkUserRequests()

        verify(gitHubIntegration, never()).addKlibsIssueComment(any(), any())
        verify(gitHubIntegration, never()).addKlibsIssueLabel(any(), any())

        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())
    }

    @Test
    fun `should process first request and mark exact subsequent requests as duplicate`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(
                listOf(
                    issue(101, body("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2")),
                    issue(102, body("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2"))
                )
            )

        uut.checkUserRequests()

        verify(userRequestIndexingService, times(1)).indexUserRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2")
        verify(gitHubIntegration).addKlibsIssueComment(eq(101), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(101, processedLabel)
        verify(gitHubIntegration).addKlibsIssueComment(eq(102), argThat { contains("duplicate of #101") })
        verify(gitHubIntegration).addKlibsIssueLabel(102, processedLabel)
    }

    @Test
    fun `should sort requests to process 'all versions' first and mark specific versions as duplicates`() {
        assertEquals(defaultTimestamp(), mavenCentralLogRepository.retrieveUserRequestCheckTimestamp())

        whenever(gitHubIntegration.getKlibsIssuesByLabel(requestLabel, defaultTimestamp()))
            .thenReturn(
                listOf(
                    issue(101, body("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2")),
                    issue(102, body("org.jetbrains.kotlinx", "kotlinx-coroutines-core", null))
                )
            )

        uut.checkUserRequests()

        verify(userRequestIndexingService).indexUserRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", null)
        verify(gitHubIntegration).addKlibsIssueComment(eq(102), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(102, processedLabel)

        verify(userRequestIndexingService, never()).indexUserRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2")
        verify(gitHubIntegration).addKlibsIssueComment(eq(101), argThat { contains("duplicate of #102") })
        verify(gitHubIntegration).addKlibsIssueLabel(101, processedLabel)
    }

    private fun verifyTimestampWasUpdated() {
        val retrieved = mavenCentralLogRepository.retrieveUserRequestCheckTimestamp()
        val now = Instant.now()
        assert(retrieved >= now.minusSeconds(60) && retrieved <= now)
    }
}