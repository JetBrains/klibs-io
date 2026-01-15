package io.klibs.core.search

import SmokeTestBase
import io.klibs.core.project.tags.TagData
import io.klibs.core.project.tags.TagService
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get

@ActiveProfiles("test")
class TagsTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var tagService: TagService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should return tags`() {
        // Arrange
        val prefix = "bitcoin-kmp"

        val tags = listOf("Service SDK", "Crypto")

        // Act & Assert
        mockMvc.get("/search/projects") {
            param("query", prefix)
            param("sort", "relevance")
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

        mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("tags", *tags.toTypedArray())
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
        val tags = listOf("Service SDK", "Crypto")

        val expectedProjectNamesList = listOf("bitcoin-kmp")

        mockMvc.get("/search/projects") {
            param("tags", *tags.toTypedArray())
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