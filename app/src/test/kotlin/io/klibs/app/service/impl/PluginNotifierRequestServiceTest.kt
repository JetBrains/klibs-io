package io.klibs.app.service.impl

import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIndexingRequestService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PluginNotifierRequestServiceTest {

    private val userIndexingRequestService = mockk<UserIndexingRequestService>(relaxUnitFun = true)
    private val uut = PluginNotifierRequestService(userIndexingRequestService)

    @Test
    fun `delegates valid GAV to UserIndexingRequestService`() {
        val request = PluginNotifierIndexingRequest(
            groupId = "org.example",
            artifactId = "lib",
            version = "1.0.0"
        )

        uut.processRequest(request)

        verify(exactly = 1) { userIndexingRequestService.saveGAVRequest("org.example", "lib", "1.0.0") }
    }

    @Test
    fun `throws UserRequestProcessingException for invalid groupId`() {
        val request = PluginNotifierIndexingRequest(
            groupId = "INVALID GROUP",  // spaces are invalid
            artifactId = "lib",
            version = "1.0.0"
        )

        assertThrows<UserRequestProcessingException> {
            uut.processRequest(request)
        }

        verify(exactly = 0) { userIndexingRequestService.saveGAVRequest(any(), any(), any()) }
    }

    @Test
    fun `rethrows UserRequestProcessingException from UserIndexingRequestService`() {
        val request = PluginNotifierIndexingRequest(
            groupId = "org.example",
            artifactId = "lib",
            version = "1.0.0"
        )
        every { userIndexingRequestService.saveGAVRequest(any(), any(), any()) } throws
            UserRequestProcessingException("Artifact is banned")

        assertThrows<UserRequestProcessingException> {
            uut.processRequest(request)
        }
    }
}
