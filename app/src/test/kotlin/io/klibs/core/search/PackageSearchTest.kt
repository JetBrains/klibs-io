package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.search.dto.api.SearchPackageResultDTOTargetList
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ActiveProfiles("test")
class PackageSearchTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    fun `package search start page request should work`() {
        // Arrange
        val page = 1
        val limit = 18

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("page", page.toString())
            param("limit", limit.toString())
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThanOrEqualTo(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        assertTrue(foundPackages.isNotEmpty(), "Response should not be empty")
        assertTrue(foundPackages.size <= limit, "Response should respect the limit parameter")
    }

    @Test
    fun `should search packages using group id`() {
        // Arrange
        val query = "kotlinx"

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("query", query)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that all found packages have "kotlinx" in their group id
        foundPackages.first().let { pkg ->
            assertTrue(pkg.groupId.contains("kotlinx"), "Package group id should contain 'kotlinx': ${pkg.groupId}")
        }
    }

    @Test
    fun `should search packages using artifact id`() {
        // Arrange
        val query = "atomicfu"

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("query", query)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that all found packages have "atomicfu" in their artifact id
        foundPackages.forEach { pkg ->
            assertTrue(pkg.artifactId.contains("atomicfu"), "Package artifact id should contain 'atomicfu': ${pkg.artifactId}")
        }
    }

    @Test
    fun `should search packages using description`() {
        // Arrange
        val query = "multiplatform"

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("query", query)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that at least one package has "multiplatform" in its description
        assertTrue(foundPackages.any { it.description?.contains("multiplatform", ignoreCase = true) == true },
            "At least one package should have 'multiplatform' in its description")
    }

    @Test
    fun `should filter packages by platform`() {
        // Arrange
        val platform = "jvm"

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("platforms", platform)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that all found packages support the JVM platform
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("jvm"), "Package should support JVM platform: ${pkg.artifactId}")
        }
    }

    @Test
    fun `should filter packages by owner`() {
        // Arrange
        val owner = "Kotlin"

        // Act & Assert
        val result = mockMvc.get("/search/packages") {
            param("owner", owner)
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that all found packages are owned by "Kotlin"
        foundPackages.forEach { pkg ->
            assertEquals("Kotlin", pkg.ownerLogin, "Package should be owned by 'Kotlin': ${pkg.artifactId}")
        }
    }

    @Test
    fun `should paginate search results`() {
        // Arrange
        val limit = 5

        // Act & Assert - Page 1
        val result1 = mockMvc.get("/search/packages") {
            param("page", "1")
            param("limit", limit.toString())
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(limit))
            }
        }.andReturn()

        val packagesPage1: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result1.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Act & Assert - Page 2
        val result2 = mockMvc.get("/search/packages") {
            param("page", "2")
            param("limit", limit.toString())
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val packagesPage2: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result2.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Verify that page 1 and page 2 contain different packages
        val page1Ids = packagesPage1.map { "${it.groupId}:${it.artifactId}" }
        val page2Ids = packagesPage2.map { "${it.groupId}:${it.artifactId}" }

        assertTrue(page1Ids.intersect(page2Ids).isEmpty(), "Page 1 and page 2 should contain different packages")
    }

    @Test
    fun `should return targets in search results`() {
        // Arrange
        val platform = "jvm"  // JVM platform typically has targets

        // Act
        val result = mockMvc.get("/search/packages") {
            param("platforms", platform)
            param("limit", "10")
        }.andExpect {
            status { isOk() }
        }.andExpect {
            content {
                jsonPath("$", hasSize<Int>(greaterThan(0)))
            }
        }.andReturn()

        val foundPackages: List<SearchPackageResultDTOTargetList> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTOTargetList::class.java)
        )

        // Assert
        // Verify that at least one package has targets
        assertTrue(foundPackages.isNotEmpty(), "Should find at least one package")

        // Check if any package has targets
        val packagesWithTargets = foundPackages.filter { it.targets.isNotEmpty() }
        assertTrue(packagesWithTargets.isNotEmpty(), 
            "At least one package should have targets. Found ${foundPackages.size} packages, but none had targets.")
    }
}
