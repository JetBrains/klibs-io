package io.klibs.app.controller

import SmokeTestBase
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.app.api.UpdatePackageDescriptionRequest
import io.klibs.app.api.UpdateProjectDescriptionRequest
import io.klibs.app.api.UpdateProjectTagsRequest
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.core.project.ProjectService
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.search.service.SearchService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.patch
import kotlin.test.assertEquals

class ContentUpdateControllerAsyncTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var searchService: SearchService

    @MockitoBean
    private lateinit var projectService: ProjectService

    @MockitoBean
    private lateinit var packageDescriptionService: PackageDescriptionService

    @Test
    fun `updateProjectDescription should return immediately and call refreshSearchViewsAsync`() {
        val request = UpdateProjectDescriptionRequest("test-project", "test-owner", "New description")
        mockMvc.patch("/content/project/description") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNoContent() }
        }

        verify(projectService).updateProjectDescription("test-project", "test-owner", "New description")
        verify(searchService).refreshSearchViewsAsync()
    }

    @Test
    fun `updateProjectTags should return immediately and call refreshSearchViewsAsync`() {
        val request = UpdateProjectTagsRequest("test-project", "test-owner", listOf("tag1", "tag2"))
        whenever(projectService.updateProjectTags(any(), any(), any(), any())).thenReturn(listOf("tag1", "tag2"))

        mockMvc.patch("/content/project/tags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        verify(projectService).updateProjectTags("test-project", "test-owner", listOf("tag1", "tag2"), TagOrigin.USER)
        verify(searchService).refreshSearchViewsAsync()
    }

    @Test
    fun `should update description for a specific package directly`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "1.0.0"
        val userProvidedDescription = "This is a user-provided description for the test library."
        val requestBody = UpdatePackageDescriptionRequest(groupId, artifactId, version, userProvidedDescription)

        whenever(
            packageDescriptionService.updatePackageDescription(
                groupId,
                artifactId,
                version,
                userProvidedDescription
            )
        )
            .thenReturn(userProvidedDescription)

        mockMvc.patch("/content/package/description") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }
            .andExpect {
                status { isOk() }
                content { string(userProvidedDescription) }
            }

        verify(packageDescriptionService).updatePackageDescription(
            groupId,
            artifactId,
            version,
            userProvidedDescription
        )
        verify(searchService).refreshSearchViewsAsync()
    }

    @Test
    fun `updateProjectTags should return 400 when project service throws IllegalArgumentException`() {
        val request =
            UpdateProjectTagsRequest("test-readme-repo", "test-readme-user", listOf("invalid tag!", "@badtag"))

        whenever(
            projectService.updateProjectTags(
                any(),
                any(),
                any(),
                any()
            )
        ).thenThrow(IllegalArgumentException("Invalid update tag request."))

        val result = mockMvc.patch("/content/project/tags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        val response =
            objectMapper.readValue(result.response.contentAsString, object : TypeReference<Map<String, String>>() {})

        val expected = mapOf("error" to "Invalid update tag request.")
        assertEquals(expected, response)
    }

}
