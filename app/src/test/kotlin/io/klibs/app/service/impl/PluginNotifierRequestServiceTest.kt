package io.klibs.app.service.impl

import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIndexingRequestService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever

class PluginNotifierRequestServiceTest {

    private val userIndexingRequestService: UserIndexingRequestService = mock()
    private val uut = PluginNotifierRequestService(userIndexingRequestService)

    @Test
    fun `delegates valid GAV to UserIndexingRequestService`() {
        val request = PluginNotifierIndexingRequest(
            groupId = "org.example",
            artifactId = "lib",
            version = "1.0.0"
        )

        uut.processRequest(request)

        verify(userIndexingRequestService).saveGAVRequest("org.example", "lib", "1.0.0")
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

        verify(userIndexingRequestService, never()).saveGAVRequest(any(), any(), any())
    }

    @Test
    fun `rethrows UserRequestProcessingException from UserIndexingRequestService`() {
        val request = PluginNotifierIndexingRequest(
            groupId = "org.example",
            artifactId = "lib",
            version = "1.0.0"
        )
        doThrow(UserRequestProcessingException("Artifact is banned"))
            .whenever(userIndexingRequestService).saveGAVRequest(any(), any(), any())

        assertThrows<UserRequestProcessingException> {
            uut.processRequest(request)
        }
    }
}
