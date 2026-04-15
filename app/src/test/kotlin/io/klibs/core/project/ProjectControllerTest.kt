package io.klibs.core.project

import BaseUnitWithDbLayerTest
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.api.PackageOverviewResponse
import io.klibs.core.search.service.SearchService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
class ProjectControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mockMvc : MockMvc

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @Sql(scripts = ["classpath:sql/ProjectControllerTest/seed.sql"])
    fun `should return descriptions for packages if present`() {
        // Act & Assert
        val result = mockMvc.get("/project/Kotlin/kotlinx-atomicfu/packages")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val packages: List<PackageOverviewResponse> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, PackageOverviewResponse::class.java)
        )

        // Verify that we have the expected number of packages
        assertEquals(1, packages.size, "Expected 1 package in the response")

        // Find the package with description
        val packageWithDescription = packages.find { it.artifactId == "atomicfu" }
        assertNotNull(packageWithDescription, "Package with description should be present")
        assertNotNull(packageWithDescription.description, "Description should not be null")
        assertEquals("AtomicFU utilities", packageWithDescription.description)
    }

    @Test
    @Sql(scripts = ["classpath:sql/ProjectControllerTest/seed.sql"])
    fun `should return 200 when project details are found`() {
        // Act & Assert
        mockMvc.get("/project/Kotlin/kotlinx-atomicfu/details")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("kotlinx-atomicfu") }
                jsonPath("$.ownerLogin") { value("Kotlin") }
            }
    }

    @Test
    @Sql(scripts = ["classpath:sql/ProjectControllerTest/seed.sql"])
    fun `should return 404 when project details are not found`() {
        // Act & Assert
        mockMvc.get("/project/Kotlin/non-existent-project/details")
            .andExpect {
                status { isNotFound() }
            }
    }
}
