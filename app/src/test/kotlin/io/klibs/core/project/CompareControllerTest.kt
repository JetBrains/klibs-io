package io.klibs.core.project

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
class CompareControllerTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return 400 when more than 10 projects requested`() {
        val projects = (1..11).map { mapOf("ownerLogin" to "owner$it", "projectName" to "project$it") }
        val request = mapOf("projects" to projects)

        mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return empty list when no projects requested`() {
        val request = mapOf("projects" to emptyList<Any>())

        mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            jsonPath("$", hasSize<Any>(0))
        }
    }

    @Test
    fun `should return null for unknown project instead of 404`() {
        val request = mapOf("projects" to listOf(
            mapOf("ownerLogin" to "nonexistent-owner-xyz", "projectName" to "nonexistent-project-xyz")
        ))

        mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            jsonPath("$", hasSize<Any>(1))
        }.andExpect {
            jsonPath("$[0]", nullValue())
        }
    }
}
