package io.klibs.app.controller

import SmokeTestBase
import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.impl.PluginNotifierRequestService
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class PluginNotifierIndexingRequestControllerTest : SmokeTestBase() {

    @MockitoBean
    private lateinit var pluginNotifierRequestService: PluginNotifierRequestService

    @Test
    fun `accepts a valid indexing request from notifier and dispatches to the service`() {
        val payload = """
            {"groupId": "org.example", "artifactId": "lib", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            content { string("Indexing request has been successfully added to the klibs.io queue") }
        }

        verify(pluginNotifierRequestService).processRequest(
            argThat<PluginNotifierIndexingRequest> {
                groupId == "org.example" && artifactId == "lib" && version == "1.0.0"
            }
        )
    }

    @Test
    fun `returns 400 when groupId is blank`() {
        val payload = """
            {"groupId": "", "artifactId": "lib", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            content { string("{\"groupId\":\"Group ID cannot be blank\"}") }
        }

        verify(pluginNotifierRequestService, never()).processRequest(any())
    }

    @Test
    fun `returns 400 when groupId is missing`() {
        val payload = """
            {"artifactId": "lib", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
        }

        verify(pluginNotifierRequestService, never()).processRequest(any())
    }

    @Test
    fun `returns 400 when artifactId is missing`() {
        val payload = """
            {"groupId": "org.example", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
        }

        verify(pluginNotifierRequestService, never()).processRequest(any())
    }

    @Test
    fun `returns 400 when version is missing`() {
        val payload = """
            {"groupId": "org.example", "artifactId": "lib"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
        }

        verify(pluginNotifierRequestService, never()).processRequest(any())
    }

    @Test
    fun `returns 400 when service throws UserRequestProcessingException`() {
        doThrow(UserRequestProcessingException("Artifact is banned"))
            .whenever(pluginNotifierRequestService).processRequest(any())

        val payload = """
            {"groupId": "org.example", "artifactId": "lib", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            content { string("Indexing request could not be processed by klibs.io: Artifact is banned") }
        }
    }

    @Test
    fun `returns 500 when service throws unexpected exception`() {
        doThrow(RuntimeException("Unexpected failure"))
            .whenever(pluginNotifierRequestService).processRequest(any())

        val payload = """
            {"groupId": "org.example", "artifactId": "lib", "version": "1.0.0"}
        """.trimIndent()

        mockMvc.post("/notify/artifacts") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isInternalServerError() }
            content { string("Indexing request failed due to internal klibs.io error") }
        }
    }
}
