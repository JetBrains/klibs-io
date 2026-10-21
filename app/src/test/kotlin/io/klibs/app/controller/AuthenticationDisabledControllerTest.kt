package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthenticationDisabledControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `auth api is absent when authentication is disabled`() {
        mockMvc.get("/auth/current-user")
            .andExpect { status { isNotFound() } }
        mockMvc.post("/auth/sign-out")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `cors remains disabled when authentication is disabled`() {
        mockMvc.perform(
            options("/")
                .header(HttpHeaders.ORIGIN, "https://frontend.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        )
            .andExpect(status().isUnauthorized)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }
}
