package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.project.ProjectDetailsDTO
import io.klibs.core.project.enums.MarkerType
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import kotlin.collections.List
import kotlin.test.assertContains
import kotlin.test.assertEquals

@ActiveProfiles("test")
class SearchTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should search projects using prefix`() {
        // Arrange
        val prefix = "kotl"

        val expectedProjectNamesList = listOf("kotlin-wrappers", "kotlinx-atomicfu", "coil")

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", prefix)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundProjects: List<SearchProjectResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )

        expectedProjectNamesList.forEachIndexed { index, expectedName ->
            assertEquals(
                foundProjects[index].name,
                expectedName,
                "Project `$expectedName` is not on the `$index` position. On this position is `${foundProjects[index].name}`."
            )
        }
    }

    @Test
    fun `multi-word prefix query`() {
        // Arrange
        val prefix = "kotlin aw"

        val expectedProjectNamesList = listOf("aws-sdk-kotlin")

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", prefix)
            param("sort", "relevance")
            param("limit", "5")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val content = result.response.contentAsString
        expectedProjectNamesList.onEach {
            assertContains(content, it)
        }
    }

    @Test
    fun `find exact matches first`() {
        // Arrange
        val prefix = "kmp"

        val expectedProjectNamesList = listOf(
            "kmp-core",
            "bitcoin-kmp",
            "secp256k1-kmp",
            "twirp-kmp",
            "kmp-commons",
        )

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", prefix)
            param("sort", "relevance")
            param("limit", "5")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundProjects: List<SearchProjectResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )
        assertEquals(foundProjects.size, expectedProjectNamesList.size)
        assertEquals(expectedProjectNamesList.toSet(), foundProjects.map { it.name }.toSet())
    }

    @Test
    fun `should search with dashes`() {
        // Arrange
        val query = "http-client-engine-crt"

        val expectedProjectNamesList = listOf("smithy-kotlin")

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("limit", "10")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val content = result.response.contentAsString
        expectedProjectNamesList.onEach {
            assertContains(content, it)
        }
    }

    @Test
    fun `aws-sdk-kotlin`() {
        // Arrange
        val query = "aws-sdk-kotlin"

        val expectedProjectNamesList = listOf("aws-sdk-kotlin")

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("limit", "5")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val content = result.response.contentAsString
        expectedProjectNamesList.onEach {
            assertContains(content, it)
        }
    }

    @Test
    fun `kotlin-wrappers`() {
        // Arrange
        val query = "kotlin-wrappers"

        val expectedProjectNamesList = listOf("kotlin-wrappers")

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("limit", "5")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val content = result.response.contentAsString
        expectedProjectNamesList.onEach {
            assertContains(content, it)
        }
    }

    @DisplayName("KTL-2109: search should work with dash at the end of the query")
    @Test
    fun testShouldWorkWithDashAtTheEndOfTheQuery() {
        // Arrange
        val query = "coil-"

        // Act & Assert
        val result = mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("limit", "10")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundProjects: List<SearchProjectResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )
        assertEquals(foundProjects.first().ownerLogin, "coil-kt")
    }

    @DisplayName("KTL-2004: search results should include project markers")
    @Test
    fun testSearchResultsIncludeProjectMarkers() {
        val query = "kotlinx-atomicfu"

        val result = mockMvc.get("/search/projects") {
            param("query", query)
            param("sort", "relevance")
            param("limit", "1")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundProjects: List<SearchProjectResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )

        // Verify that the markers field is not null and is a list
        assertEquals(listOf(MarkerType.FEATURED.name, MarkerType.GRANT_WINNER_2024.name), foundProjects.first().markers)
    }

    @Test
    fun `the number of platforms should be consistent for the project details and project search`() {
        val projectName = "kotlin-wrappers"

        val projectDetailsResponse = mockMvc.get("/project/JetBrains/${projectName}/details")
            .andExpect {
                status { isOk() }
            }
            .andReturn()


        val projectDetailsDto: ProjectDetailsDTO = objectMapper.readValue(
            projectDetailsResponse.response.contentAsString,
            ProjectDetailsDTO::class.java
        )

        val projectSearchResponse = mockMvc.get("/search/projects") {
            param("query", projectName)
            param("sort", "relevance")
            param("limit", "1")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(equalTo(1)))
            }
        }.andReturn()

        val projectSearchDto: List<SearchProjectResultDTO> = objectMapper.readValue(
            projectSearchResponse.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )

        assertEquals(projectSearchDto.first().platforms.sorted(), projectDetailsDto.platforms.sorted())
    }
}
