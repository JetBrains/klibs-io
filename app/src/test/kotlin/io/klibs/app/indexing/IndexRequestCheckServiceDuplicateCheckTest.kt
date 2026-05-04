package io.klibs.app.indexing

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IndexRequestCheckServiceDuplicateCheckTest {

    private fun uut() = IndexRequestCheckService(
        gitHubIntegration = mock(),
        requestIndexingService = mock(),
        mavenCentralLogRepository = mock(),
        requestLabel = "index-request",
        processedLabel = "triaged",
    )

    private val baseRequest = IndexRequestCheckService.ParsedRequest(
        groupId = "org.jetbrains.kotlinx",
        artifactId = "kotlinx-coroutines-core",
        version = "1.10.2"
    )

    @Test
    fun `should return null when processed requests list is empty`() {
        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, emptyList())

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return null when there are no matching processed requests`() {
        val processedRequests = listOf(
            IndexRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0") to 10,
            IndexRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "different-artifact", "1.10.2") to 11
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return null when previous request was for a specific version and current is for a different one`() {
        val processedRequests = listOf(
            IndexRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.0.0") to 10,
            IndexRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0") to 11
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertNull(duplicateNumber)
    }

    @Test
    fun `should return issue number when exact duplicate is found`() {
        val processedRequests = listOf(
            IndexRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2") to 10,
            IndexRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0") to 11
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertEquals(10, duplicateNumber)
    }

    @Test
    fun `should return issue number when previous request was for all versions`() {
        val processedRequests = listOf(
            IndexRequestCheckService.ParsedRequest("org.jetbrains.kotlinx", "kotlinx-coroutines-core", null) to 10,
            IndexRequestCheckService.ParsedRequest("other.group", "other-artifact", "1.0.0") to 11
        )

        val duplicateNumber = uut().findDuplicateIssueNumber(baseRequest, processedRequests)

        assertEquals(10, duplicateNumber)
    }
}