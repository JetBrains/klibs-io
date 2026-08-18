package io.klibs.app.controller

import SmokeTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class NotifyControllerTest : SmokeTestBase() {

    @Test
    fun `echoes the number of received artifacts`() {
        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                [
                  {"groupId": "io.github.test", "artifactId": "fibonacci", "version": "1.0.0"},
                  {"groupId": "io.github.test", "artifactId": "lucas", "version": "2.0.0"}
                ]
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { string("Received 2 artifacts") }
        }
    }
}
