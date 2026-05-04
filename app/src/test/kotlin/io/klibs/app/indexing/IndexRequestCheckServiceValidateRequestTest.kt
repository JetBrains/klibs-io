package io.klibs.app.indexing

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IndexRequestCheckServiceValidateRequestTest {

    private fun uut() = IndexRequestCheckService(
        gitHubIntegration = mock(),
        requestIndexingService = mock(),
        mavenCentralLogRepository = mock(),
        requestLabel = "index-request",
        processedLabel = "triaged",
    )

    @Test
    fun `should return null for correct input`() {
        val parsed = IndexRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx_test",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )
        val error = uut().validateRequest(parsed)

        assertNull(error)
    }

    @Test
    fun `should return error when group id contains invalid characters`() {
        val parsed = IndexRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains/kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2"
        )
        val error = uut().validateRequest(parsed)

        assertNotNull(error)
        assertEquals("Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed.", error)
    }

    @Test
    fun `should return error when artifact id contains spaces`() {
        val parsed = IndexRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx coroutines core",
            version = "1.10.2"
        )
        val error = uut().validateRequest(parsed)

        assertNotNull(error)
        assertEquals("Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed.", error)
    }

    @Test
    fun `should allow special characters in version`() {
        val parsed = IndexRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = "1.10.2-revision._$~=+:"
        )
        val error = uut().validateRequest(parsed)

        assertNull(error)
    }

    @Test
    fun `should allow null version for indexing all versions`() {
        val parsed = IndexRequestCheckService.ParsedRequest(
            groupId = "org.jetbrains.kotlinx",
            artifactId = "kotlinx-coroutines-core",
            version = null
        )
        val error = uut().validateRequest(parsed)

        assertNull(error)
    }

}