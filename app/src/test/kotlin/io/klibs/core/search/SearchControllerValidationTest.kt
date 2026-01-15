package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.dto.api.SearchProjectsRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
class SearchControllerValidationTest : SmokeTestBase() {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("Should return 400 Bad Request for invalid platform")
    fun testInvalidPlatform() {
        mockMvc.get("/search/projects") {
            param("platforms", "invalid-platform")
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }.andExpect {
            content {
                jsonPath("$.error") { exists() }
                jsonPath("$.error") { value("Invalid platform: invalid-platform") }
            }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid sort option")
    fun testInvalidSortOption() {
        mockMvc.get("/search/projects") {
            param("sort", "invalid-sort")
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }.andExpect {
            content {
                jsonPath("$.error") { exists() }
                jsonPath("$.error") { value("Unexpected sort option: invalid-sort") }
            }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid sort option in POST request")
    fun testInvalidSortOptionInPostRequest() {
        val searchRequest = SearchProjectsRequest(
            sortBy = "invalid-sort"
        )

        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }.andExpect {
            content {
                jsonPath("$.error") { exists() }
                jsonPath("$.error") { value("Unexpected sort option: invalid-sort") }
            }
        }
    }
}