package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.service.SearchService
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
class ReadmeSearchTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should find project by minimized readme content`() {
        val searchRequest = SearchProjectsRequest(
            query = "unique readme content",
            sortBy = "relevance",
        )

        // Act & Assert
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$[0].name", `is`("test-readme-repo"))
            }
        }
    }
}
