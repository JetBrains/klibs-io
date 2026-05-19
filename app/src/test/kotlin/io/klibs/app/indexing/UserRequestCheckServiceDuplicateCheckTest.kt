package io.klibs.app.indexing

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRequestCheckServiceDuplicateCheckTest {

    private fun uut() = UserRequestCheckService(
        gitHubIntegration = mock(),
        userRequestIndexingService = mock(),
        mavenCentralLogRepository = mock(),
        requestLabel = "index-request",
        processedLabel = "triaged",
    )

    @Test
    fun `should return null when processed requests list is empty`() {
        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )
        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, emptyList())

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return null when there are no matching processed requests`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0"),
                10
            ),
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "different-artifact", "1.10.2"),
                11
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return null when previous request was for a specific version and current is for a different one`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.0.0"),
                10
            ),
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0"),
                11
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return issue number when exact duplicate is found and version is specified`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2"),
                10
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertEquals(10, duplicateNumber)
    }

    @Test
    fun `should return issue number when exact duplicate is found and version is null`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", null),
                10
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = null
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertEquals(10, duplicateNumber)
    }

    @Test
    fun `should return null when current request has version and processed request version is null`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", null),
                10
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return null when current request version is null and processed request has version`() {
        val processedRequests = listOf(
            UserRequestCheckService.ProcessedRequestInfo(
                UserRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2"),
                10
            )
        )

        val baseRequest = UserRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = null
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }
}