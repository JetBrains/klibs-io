package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.enums.JETBRAINS_AND_GOOGLE_MARKER
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
    @DisplayName("Should return 400 Bad Request for invalid sort option")
    fun testInvalidSortOption() {
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

    @Test
    @DisplayName("Should accept the owner category marker")
    fun testValidCategorySelector() {
        val searchRequest = SearchProjectsRequest(markers = listOf(JETBRAINS_AND_GOOGLE_MARKER))

        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @DisplayName("Category response exposes the owner category marker")
    fun testCategorySelectorResponse() {
        mockMvc.get("/categories/projects") {
            param("limit", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.categories[0].category.id") { doesNotExist() }
            jsonPath("$.categories[0].category.markers[0]") { value(JETBRAINS_AND_GOOGLE_MARKER) }
            jsonPath("$.categories[0].category.ownerLogins") { doesNotExist() }
            jsonPath("$.categories[0].category.selector") { doesNotExist() }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request when page exceeds 50")
    fun testPageAboveCap() {
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SearchProjectsRequest())
            param("page", "51")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request when page exceeds 50 in POST request")
    fun testPageAboveCapInPostRequest() {
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SearchProjectsRequest())
            param("page", "51")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("Should accept page 50")
    fun testPageAtCap() {
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SearchProjectsRequest())
            param("page", "50")
            param("limit", "10")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request when page is 0 in POST request")
    fun testPageBelowFirstInPostRequest() {
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SearchProjectsRequest())
            param("page", "0")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
