package io.klibs.core.pckg.controller

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.core.search.service.SearchService
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageDescriptionControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkSpyBean
    private lateinit var packageDescriptionService: PackageDescriptionService

    @MockkBean
    private lateinit var packageDescriptionGenerator: PackageDescriptionGenerator

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionControllerTest/insert-package-with-specific-version.sql"])
    fun `should generate description for a specific package with all parameters`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "1.0.0"
        val expectedDescription = "This is a test library for demonstration purposes."

        every {
            packageDescriptionGenerator.generatePackageDescription(
                groupId,
                artifactId,
                version
            )
        } returns expectedDescription

        val result = mockMvc.get("/package-description/$groupId/$artifactId/$version")
            .andExpect {
                status { isOk() }
                content { string(expectedDescription) }
            }
            .andReturn()

        assertEquals(expectedDescription, result.response.contentAsString)
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionControllerTest/insert-packages-with-same-groupid.sql"])
    fun `should generate description for a package with only groupId`() {
        val groupId = "org.example"
        val artifactId1 = "test-library"
        val artifactId2 = "test-utils"
        val version = "1.0.0"
        val expectedDescription = "This is a description for the org.example group."

        every {
            packageDescriptionGenerator.generatePackageDescription(
                groupId,
                artifactId1,
                version
            )
        } returns expectedDescription

        every {
            packageDescriptionGenerator.generatePackageDescription(
                groupId,
                artifactId2,
                version
            )
        } returns expectedDescription

        val result = mockMvc.get("/package-description/$groupId")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        assertTrue(result.response.contentAsString.contains("Updated descriptions for 2 packages with groupId=$groupId"))
        assertTrue(result.response.contentAsString.contains("$groupId:$artifactId1:$version"))
        assertTrue(result.response.contentAsString.contains("$groupId:$artifactId2:$version"))
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionControllerTest/insert-package-with-groupid-artifactid.sql"])
    fun `should generate description for a package with groupId and artifactId but no version`() {
        // Arrange
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "2.0.0" // Latest version
        val expectedDescription = "This is a description for the org.example:test-library package."

        every {
            packageDescriptionGenerator.generatePackageDescription(
                groupId,
                artifactId,
                version
            )
        } returns expectedDescription

        val result = mockMvc.get("/package-description/$groupId/$artifactId")
            .andExpect {
                status { isOk() }
                content { string(expectedDescription) }
            }
            .andReturn()

        assertEquals(expectedDescription, result.response.contentAsString)
    }

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionServiceTest/insert-packages-with-duplicate-descriptions.sql"])
    fun `should start generating unique descriptions asynchronously and return success message immediately`() {
        // Stub the coroutine method
        justRun { packageDescriptionService.generateUniqueDescriptions() }

        val result = mockMvc.post("/package-description/generate-unique")
            .andExpect {
                status { isOk() }
                content { string("Unique descriptions generation started successfully") }
            }
            .andReturn()

        assertEquals("Unique descriptions generation started successfully", result.response.contentAsString)

        // We use timeout(1000) to wait up to 1 second for the method to be called
        // This is necessary because the method is called in a separate thread
        verify(exactly = 1, timeout = 1000) { packageDescriptionService.generateUniqueDescriptions() }
    }
}
