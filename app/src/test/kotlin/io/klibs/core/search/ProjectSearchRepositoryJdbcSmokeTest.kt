package io.klibs.core.search

import SmokeTestBase
import io.klibs.core.pckg.model.TargetGroup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@ActiveProfiles("test")
class ProjectSearchRepositoryJdbcSmokeTest : SmokeTestBase() {
    @Autowired
    private lateinit var projectSearchRepository: ProjectSearchRepositoryJdbc

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Should verify that correct plaftorm is present in search results when using target filters")
    fun testCorrectPlatformInTargetFilterSearchResults() {
        // Arrange
        val targetFilters = mapOf(
            TargetGroup.JVM to emptySet<String>()
        )

        // Act
        val searchResults = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = targetFilters,
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = emptyList(),
            page = 1,
            limit = 10
        )

        // Assert that the search results are not empty
        assertTrue(searchResults.isNotEmpty(), "Search results should not be empty")

        // Assert that at least one project has the JVM platform
        assertTrue(
            searchResults.all { project -> project.platforms.any { it.serializableName == "jvm" } },
            "No projects found with JVM platform. At least one project should have the JVM platform."
        )
    }

    @Test
    @DisplayName("Should verify that all required JVM targets are present in search results when using target filters")
    fun testTargetsInSearchResultsJVM() {
        // Arrange
        val targetFilters = mapOf(
            TargetGroup.JVM to setOf("11", "17"),
        )

        // Act
        val searchResults = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = targetFilters,
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = emptyList(),
            page = 1,
            limit = 10
        )

        // Assert that the search results are not empty
        assertTrue(searchResults.isNotEmpty(), "Search results should not be empty")

        // Assert that at least one project has the JVM platform
        assertTrue(
            searchResults.all { project ->
                val minRequiredTarget = targetFilters[TargetGroup.JVM]!!.min()
                val requiredTargets = TargetGroup.JVM.targets.filter { it >= minRequiredTarget } .map { "${TargetGroup.JVM.platformName}_$it" }
                requiredTargets.any { it in project.targets }
            },
            "Required targets not found in search results. All required targets should be present."
        )
    }

    @Test
    @DisplayName("Should verify that all required JVM and IOS targets are present in search results when using target filters")
    fun testTargetsInSearchResultsJVMandIOS() {
        // Arrange
        val targetFilters = mapOf(
            TargetGroup.JVM to setOf("11"),
            TargetGroup.IOS to emptySet(),
        )

        // Act
        val searchResults = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = targetFilters,
            ownerLogin = null,
            sortBy = SearchSort.MOST_STARS,
            tags = emptyList(),
            markers = emptyList(),
            page = 1,
            limit = 10
        )

        // Assert that the search results are not empty
        assertTrue(searchResults.isNotEmpty(), "Search results should not be empty")

        assertTrue(
            searchResults.all { project ->
                val minRequiredTarget = targetFilters[TargetGroup.JVM]!!.min()
                val requiredTargets = TargetGroup.JVM.targets.filter { it >= minRequiredTarget } .map { "${TargetGroup.JVM.platformName}_$it" }
                requiredTargets.any { it in project.targets }
            },
            "Required JVM targets not found in search results."
        )

        assertTrue(
            searchResults.all { project ->
                project.targets.any { it in TargetGroup.IOS.targets.map { "${TargetGroup.IOS.platformName}_$it" } }
            },
            "Required IOS targets not found in search results."
        )
    }
}
