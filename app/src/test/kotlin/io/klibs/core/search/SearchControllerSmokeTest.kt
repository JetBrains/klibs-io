package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import io.klibs.core.search.dto.api.SearchProjectsRequest
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.post
import kotlin.test.assertTrue

@ActiveProfiles("test")
class SearchControllerSmokeTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Should filter projects by JavaScript target")
    fun testJavaScriptTargetFilter() {
        // Arrange
        val searchRequest = SearchProjectsRequest(
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
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

        // Verify that all returned projects support JavaScript platform
        foundProjects.forEach { project ->
            assertTrue(project.platforms.contains("js"), 
                "Project ${project.name} does not support JavaScript platform")
        }
    }

    @Test
    @DisplayName("Should filter projects by JavaScript target with specific target values")
    fun testJavaScriptTargetFilterWithSpecificValues() {
        // Arrange
        val searchRequest = SearchProjectsRequest(
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
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

        // Verify that all returned projects support JavaScript platform
        foundProjects.forEach { project ->
            assertTrue(project.platforms.contains("js"), 
                "Project ${project.name} does not support JavaScript platform")
        }
    }

    @Test
    @DisplayName("Should filter projects by JavaScript target combined with query")
    fun testJavaScriptTargetFilterWithQuery() {
        // Arrange
        val searchRequest = SearchProjectsRequest(
            query = "kotlin",
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
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

        // Verify that all returned projects support JavaScript platform and match the query
        foundProjects.forEach { project ->
            assertTrue(project.platforms.contains("js"), 
                "Project ${project.name} does not support JavaScript platform")
        }
    }

    @Test
    @DisplayName("Should filter projects by JavaScript target combined with other target filters")
    fun testJavaScriptTargetFilterWithOtherTargets() {
        // Arrange
        val searchRequest = SearchProjectsRequest(
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy"),
                TargetGroup.JVM to setOf("11", "17")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
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

        // Verify that all returned projects support both JavaScript platform and JVM
        foundProjects.forEach { project ->
            assertTrue(project.platforms.contains("js"), 
                "Project ${project.name} does not support JavaScript platform")
            assertTrue(project.platforms.contains("jvm"), 
                "Project ${project.name} does not support JVM platform")
        }
    }

    @Test
    @DisplayName("Should filter projects by JVM target")
    fun testJvmTargetFilter() {
        // Arrange
        val searchRequest = SearchProjectsRequest(
            targetFilters = mapOf(
                TargetGroup.JVM to setOf("11", "17")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
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

        // Verify that all returned projects support JVM platform
        foundProjects.forEach { project ->
            assertTrue(project.platforms.contains("jvm"), 
                "Project ${project.name} does not support JVM platform")
        }
    }
}
