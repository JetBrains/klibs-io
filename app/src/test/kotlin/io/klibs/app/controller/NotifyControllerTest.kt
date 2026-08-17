package io.klibs.app.controller

import SmokeTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class NotifyControllerTest : SmokeTestBase() {

    @Test
    fun `echoes the received form encoded body`() {
        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            param("groupId", "io.github.zwiora")
            param("artifactId", "fibonacci")
            param("version", "1.0.0")
        }.andExpect {
            status { isOk() }
            content { string("Received: groupId=io.github.zwiora&artifactId=fibonacci&version=1.0.0") }
        }
    }
}
