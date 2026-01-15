package io.klibs.core.project

import SmokeTestBase
import org.springframework.test.context.ActiveProfiles
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.api.PackageOverviewResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
class ProjectControllerTest : SmokeTestBase() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
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
    fun `should return 404 when project details are not found`() {
        // Act & Assert
        mockMvc.get("/project/Kotlin/non-existent-project/details")
            .andExpect {
                status { isNotFound() }
            }
    }
}
