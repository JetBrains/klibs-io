package io.klibs.app.controller

import SmokeTestBase
import io.klibs.app.api.UpdateProjectDescriptionRequest
import io.klibs.app.api.UpdateProjectTagsRequest
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.core.project.ProjectService
import io.klibs.core.search.SearchService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.patch
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.app.api.UpdatePackageDescriptionRequest

class ContentUpdateControllerAsyncTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var searchService: SearchService

    @MockBean
    private lateinit var projectService: ProjectService

    @MockBean
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
        `when`(projectService.updateUserTags(anyString(), anyString(), anyList())).thenReturn(listOf("tag1", "tag2"))

        mockMvc.patch("/content/project/tags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        verify(projectService).updateUserTags("test-project", "test-owner", listOf("tag1", "tag2"))
        verify(searchService).refreshSearchViewsAsync()
    }

    @Test
    fun `should update description for a specific package directly`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "1.0.0"
        val userProvidedDescription = "This is a user-provided description for the test library."
        val requestBody = UpdatePackageDescriptionRequest(userProvidedDescription)

        `when`(packageDescriptionService.updatePackageDescription(groupId, artifactId, version, userProvidedDescription))
            .thenReturn(userProvidedDescription)

        mockMvc.patch("/content/package/description/$groupId/$artifactId/$version") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }
        .andExpect {
            status { isOk() }
            content { string(userProvidedDescription) }
        }

        verify(packageDescriptionService).updatePackageDescription(groupId, artifactId, version, userProvidedDescription)
        verify(searchService).refreshSearchViewsAsync()
    }
}
