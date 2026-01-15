package io.klibs.core.search

import SmokeTestBase
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get

@ActiveProfiles("test")
class ReadmeSearchTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should find project by minimized readme content`() {
        // Act & Assert
        mockMvc.get("/search/projects") {
            param("query", "unique readme content")
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$[0].name", `is`("test-readme-repo"))
            }
        }
    }
}
