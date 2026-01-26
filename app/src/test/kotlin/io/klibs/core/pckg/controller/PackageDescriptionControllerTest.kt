package io.klibs.core.pckg.controller

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.integration.ai.PackageDescriptionGenerator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
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
    private lateinit var mockMvc: MockMvc

    @SpyBean
    private lateinit var packageDescriptionService: PackageDescriptionService

    @MockBean
    private lateinit var packageDescriptionGenerator: PackageDescriptionGenerator

    @Test
    @Sql(value = ["classpath:sql/PackageDescriptionControllerTest/insert-package-with-specific-version.sql"])
    fun `should generate description for a specific package with all parameters`() {
        val groupId = "org.example"
        val artifactId = "test-library"
        val version = "1.0.0"
        val expectedDescription = "This is a test library for demonstration purposes."

        `when`(packageDescriptionGenerator.generatePackageDescription(
            groupId,
            artifactId,
            version
        )).thenReturn(expectedDescription)

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

        `when`(packageDescriptionGenerator.generatePackageDescription(
            groupId,
            artifactId1,
            version
        )).thenReturn(expectedDescription)

        `when`(packageDescriptionGenerator.generatePackageDescription(
            groupId,
            artifactId2,
            version
        )).thenReturn(expectedDescription)

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

        `when`(packageDescriptionGenerator.generatePackageDescription(
            groupId,
            artifactId,
            version
        )).thenReturn(expectedDescription)

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
        val result = mockMvc.post("/package-description/generate-unique")
            .andExpect {
                status { isOk() }
                content { string("Unique descriptions generation started successfully") }
            }
            .andReturn()

        assertEquals("Unique descriptions generation started successfully", result.response.contentAsString)

        // We use timeout(1000) to wait up to 1 second for the method to be called
        // This is necessary because the method is called in a separate thread
        verify(packageDescriptionService, timeout(1000)).generateUniqueDescriptions()
    }
}
