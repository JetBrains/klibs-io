package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.project.tags.TagData
import io.klibs.core.project.service.TagService
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.service.SearchService
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
class TagsTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var tagService: TagService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should return tags`() {
        // Arrange
        val prefix = "bitcoin-kmp"

        val tags = listOf("Service SDK", "Crypto")

        val searchRequest = SearchProjectsRequest(
            query = prefix,
        )

        // Act & Assert
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
                jsonPath("$[0].tags", `is`(tags))
            }
        }
    }

    @Test
    fun `search with query and filter by tags`() {
        // Arrange
        val query = "kmp"

        val tags = listOf("Service SDK", "Crypto")

        val expectedProjectNamesList = listOf("bitcoin-kmp")

        val searchRequest = SearchProjectsRequest(
            query = query,
            tags = tags,
        )

        // Act & Assert
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
                expectedProjectNamesList.forEachIndexed { index, expectedName ->
                    jsonPath("$[$index].name", `is`(expectedName))
                }
            }
        }
    }

    @Test
    fun `search without query and filter by tags`() {
        // Arrange
        val tags = listOf("Service SDK", "Crypto")

        val expectedProjectNamesList = listOf("bitcoin-kmp")

        val searchRequest = SearchProjectsRequest(
            tags = tags,
        )

        // Act & Assert
        mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
                expectedProjectNamesList.forEachIndexed { index, expectedName ->
                    jsonPath("$[$index].name", `is`(expectedName))
                }
            }
        }
    }

    @Test
    fun `get tags stats`() {
        // Arrange
        val limit = 3

        val expectedTags = listOf(
            TagData("Compose UI", 39),
            TagData("Dependency Injection", 27),
            TagData("Utility", 22)
        )

        val totalProjectsCount = 52
        // Act & Assert
        mockMvc.get("/tags/stats") {
            param("limit", limit.toString())
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$.tags", hasSize<Int>(limit))
                jsonPath("$.totalProjectsCount", `is`(totalProjectsCount))
                expectedTags.forEachIndexed { index, tagData ->
                    jsonPath("$.tags[$index].tag", `is`(tagData.tag))
                    jsonPath("$.tags[$index].projectsCount", equalTo(tagData.projectsCount.toInt()))
                }
            }
        }
    }
}