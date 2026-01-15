package io.klibs.core.search

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.dto.api.SearchPackageResultDTO
import io.klibs.core.search.dto.api.SearchPackagesRequest
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
class SearchPackagesWithTargetFiltersTest : SmokeTestBase() {
    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Should filter packages by JavaScript target")
    fun testJavaScriptTargetFilter() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support JavaScript platform
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("js"), 
                "Package ${pkg.artifactId} does not support JavaScript platform")
        }
    }

    @Test
    @DisplayName("Should filter packages by JavaScript target combined with query")
    fun testJavaScriptTargetFilterWithQuery() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            query = "kotlin",
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support JavaScript platform and match the query
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("js"), 
                "Package ${pkg.artifactId} does not support JavaScript platform")

            // Check if the package matches the query (in groupId, artifactId, or description)
            val matchesQuery = pkg.groupId.contains("kotlin", ignoreCase = true) ||
                               pkg.artifactId.contains("kotlin", ignoreCase = true) ||
                               pkg.description?.contains("kotlin", ignoreCase = true) == true

            assertTrue(matchesQuery, "Package ${pkg.artifactId} does not match the query 'kotlin'")
        }
    }

    @Test
    @DisplayName("Should filter packages by JavaScript target combined with other target filters")
    fun testJavaScriptTargetFilterWithOtherTargets() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(
                TargetGroup.JavaScript to setOf("js_ir", "js_legacy"),
                TargetGroup.JVM to setOf("11", "17")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support both JavaScript platform and JVM
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("js"), 
                "Package ${pkg.artifactId} does not support JavaScript platform")
            assertTrue(pkg.platforms.contains("jvm"), 
                "Package ${pkg.artifactId} does not support JVM platform")

            // Note: Some packages might have the platform but not specific targets in the targets map
            // At least one of the target groups should be present
            assertTrue(pkg.targets.containsKey(TargetGroup.JVM),
                "Package ${pkg.artifactId} does not have JVM targets")
        }
    }

    @Test
    @DisplayName("Should filter packages by JVM target")
    fun testJvmTargetFilter() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(
                TargetGroup.JVM to setOf("11", "17")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support JVM platform
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("jvm"), 
                "Package ${pkg.artifactId} does not support JVM platform")

            // Verify that the package has JVM targets
            assertTrue(pkg.targets.containsKey(TargetGroup.JVM),
                "Package ${pkg.artifactId} does not have JVM targets")

            // Verify that the package has at least one of the specified JVM targets
            val jvmTargets = pkg.targets[TargetGroup.JVM] ?: emptySet()
            assertTrue(jvmTargets.any { it == "11" || it == "17" },
                "Package ${pkg.artifactId} does not support JVM 11 or 17")
        }
    }

    @Test
    @DisplayName("Should filter packages by Native target")
    fun testNativeTargetFilter() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(
                TargetGroup.IOS to setOf("ios_arm64", "ios_x64")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support Native platform
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("native"), 
                "Package ${pkg.artifactId} does not support Native platform")

            // Verify that the package has IOS targets
            assertTrue(pkg.targets.containsKey(TargetGroup.IOS),
                "Package ${pkg.artifactId} does not have IOS targets")

            // Verify that the package has at least one of the specified IOS targets
            val iosTargets = pkg.targets[TargetGroup.IOS] ?: emptySet()
            assertTrue(iosTargets.any { it == "ios_arm64" || it == "ios_x64" },
                "Package ${pkg.artifactId} does not support ios_arm64 or ios_x64")
        }
    }

    @Test
    @DisplayName("Should filter packages by multiple Native targets")
    fun testMultipleNativeTargetFilter() {
        // Arrange
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(
                TargetGroup.IOS to setOf("ios_arm64"),
                TargetGroup.MacOS to setOf("macos_arm64", "macos_x64")
            )
        )

        // Act & Assert
        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        // Verify that all returned packages support Native platform
        foundPackages.forEach { pkg ->
            assertTrue(pkg.platforms.contains("native"), 
                "Package ${pkg.artifactId} does not support Native platform")

            // Verify that the package has both IOS and MacOS targets
            assertTrue(pkg.targets.containsKey(TargetGroup.IOS),
                "Package ${pkg.artifactId} does not have IOS targets")
            assertTrue(pkg.targets.containsKey(TargetGroup.MacOS),
                "Package ${pkg.artifactId} does not have MacOS targets")

            // Verify that the package has the specified IOS target
            val iosTargets = pkg.targets[TargetGroup.IOS] ?: emptySet()
            assertTrue(iosTargets.contains("ios_arm64"),
                "Package ${pkg.artifactId} does not support ios_arm64")

            // Verify that the package has at least one of the specified MacOS targets
            val macosTargets = pkg.targets[TargetGroup.MacOS] ?: emptySet()
            assertTrue(macosTargets.any { it == "macos_arm64" || it == "macos_x64" },
                "Package ${pkg.artifactId} does not support macos_arm64 or macos_x64")
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request when targetFilters contain unknown target")
    fun testUnknownTargetInTargetFilters() {
        val searchRequest = SearchPackagesRequest(
            query = "anything",
            targetFilters = mapOf(TargetGroup.JVM to setOf("999"))
        )

        mockMvc.post("/search/packages") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(searchRequest)
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request when targetFilters contain wrong target group")
    fun testUnknownTargetGroupInTargetFilters() {
        val body = """
            {
            "query": "anything",
            "targetFilters": {
                "WTF": []
            }
            }
        """.trimIndent()

        mockMvc.post("/search/packages") {
            contentType = MediaType.APPLICATION_JSON
            content = body
            param("page", "1")
            param("limit", "10")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("Should return 200 with old jvm")
    fun testOldJvmTargetInTargetFilters() {
        val searchRequest = SearchPackagesRequest(
            targetFilters = mapOf(TargetGroup.JVM to setOf("9"))
        )

        val result = mockMvc.post("/search/packages") {
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

        val foundPackages: List<SearchPackageResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchPackageResultDTO::class.java)
        )

        assertTrue(foundPackages.any { it.targets.containsKey(TargetGroup.JVM) })
    }
}
