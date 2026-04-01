package io.klibs.core.project

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.project.api.CompareProjectResponse
import io.klibs.core.search.service.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
class CompareControllerTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `should return 200 with project data when all request parameters are valid`() {
        val request = mapOf(
            "projects" to listOf(
                mapOf("ownerLogin" to "JetBrains", "projectName" to "kotlin-wrappers"),
                mapOf("ownerLogin" to "Kotlin", "projectName" to "kotlinx-atomicfu")
            )
        )

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }.andReturn().response

        val results: List<CompareProjectResponse> = objectMapper.readValue(
            response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, CompareProjectResponse::class.java)
        )

        assertEquals(2, results.size)

        val projectsByName = results.associateBy { it.projectName }

        val kotlinWrappersProject = projectsByName["kotlin-wrappers"]
        assertNotNull(kotlinWrappersProject)
        assertEquals("JetBrains", kotlinWrappersProject.ownerLogin)
        assertTrue(kotlinWrappersProject.lastActivityAtMillis > 0)
        assertTrue(kotlinWrappersProject.createdAtMillis > 0)
        assertTrue(kotlinWrappersProject.platforms.isNotEmpty())
        assertNotNull(kotlinWrappersProject.kotlinVersion)

        val atomicfuProject = projectsByName["kotlinx-atomicfu"]
        assertNotNull(atomicfuProject)
        assertEquals("Kotlin", atomicfuProject.ownerLogin)
        assertTrue(atomicfuProject.lastActivityAtMillis > 0)
        assertTrue(atomicfuProject.createdAtMillis > 0)
        assertTrue(atomicfuProject.platforms.isNotEmpty())
        assertNotNull(atomicfuProject.kotlinVersion)
    }

    @Test
    fun `should return 400 when more than 10 projects requested`() {
        val projects = (1..11).map { mapOf("ownerLogin" to "owner$it", "projectName" to "project$it") }
        val request = mapOf("projects" to projects)

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().response

        assertEquals(
            "{\"projects\":\"For comparison, you can only compare up to 10 projects at once and at least 1\"}",
            response.contentAsString
        )
    }

    @Test
    fun `should return 400 when project name is blank`() {
        val projects = listOf(mapOf("ownerLogin" to "owner login", "projectName" to ""))
        val request = mapOf("projects" to projects)

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().response

        assertEquals(
            "{\"projects[0].projectName\":\"Project name cannot be blank\"}",
            response.contentAsString
        )
    }

    @Test
    fun `should return 400 when owner login is blank`() {
        val projects = listOf(mapOf("ownerLogin" to "", "projectName" to "project name"))
        val request = mapOf("projects" to projects)

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().response

        assertEquals(
            "{\"projects[0].ownerLogin\":\"Owner login cannot be blank\"}",
            response.contentAsString
        )
    }

    @Test
    fun `should return empty list when no projects requested`() {
        val request = mapOf("projects" to emptyList<Any>())

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().response

        assertEquals(
            "{\"projects\":\"For comparison, you can only compare up to 10 projects at once and at least 1\"}",
            response.contentAsString
        )
    }

    @Test
    fun `should return 400 when owner login is blank when some of the projects aren't exist`() {
        val request = mapOf(
            "projects" to listOf(
                mapOf("ownerLogin" to "nonexistent-owner-xyz", "projectName" to "nonexistent-project-xyz")
            )
        )

        val response = mockMvc.post("/compare/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().response

        assertEquals(
            "{\"error\":\"Unable to find projects from this list: [nonexistent-project-xyz]\"}",
            response.contentAsString
        )
    }
}
