package io.klibs.core.search

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ActiveProfiles("test")
class SearchMarkersFilterDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectSearchRepository: ProjectSearchRepositoryJdbc

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Search filter by single marker should return only projects having this marker")
    @Sql(value = ["classpath:sql/SearchMarkersFilterDbTest/seed.sql"])
    fun `filter by single marker`() {
        val results = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = listOf("FEATURED"),
            page = 1,
            limit = 50
        )

        assertTrue(results.isNotEmpty(), "Expected non-empty results for FEATURED marker")
        assertTrue(results.all { it.markers.contains("FEATURED") }, "All results must contain FEATURED marker")
    }

    @Test
    @DisplayName("Search filter by multiple markers should apply OR logic (any marker may be present)")
    @Sql(value = ["classpath:sql/SearchMarkersFilterDbTest/seed.sql"])
    fun `filter by multiple markers uses OR`() {
        val results = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = listOf("FEATURED", "GRANT_WINNER_2024"),
            page = 1,
            limit = 50
        )

        // OR semantics: any project having at least one of the markers should be returned
        assertEquals(3, results.size, "All three projects should match at least one of the markers")
        assertTrue(results.all { it.markers.any { m -> m == "FEATURED" || m == "GRANT_WINNER_2024" } })

        // Also ensure that the project that has both markers is included ("all of" still matches OR)
        assertTrue(
            results.any { it.markers.containsAll(listOf("FEATURED", "GRANT_WINNER_2024")) },
            "Expected a project that has both markers to be present in OR results"
        )
    }

    @Test
    @DisplayName("Unknown marker should yield empty results")
    @Sql(value = ["classpath:sql/SearchMarkersFilterDbTest/seed.sql"])
    fun `unknown marker yields empty`() {
        val results = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = listOf("NON_EXISTENT_MARKER"),
            page = 1,
            limit = 10
        )

        assertTrue(results.isEmpty(), "No projects should be returned for an unknown marker")
    }

    @Test
    @DisplayName("One-of case: mix existing and non-existing marker should return projects with the existing one")
    @Sql(value = ["classpath:sql/SearchMarkersFilterDbTest/seed.sql"])
    fun `one of markers when list contains non existing`() {
        val results = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.RELEVANCY,
            tags = emptyList(),
            markers = listOf("FEATURED", "NON_EXISTENT_MARKER"),
            page = 1,
            limit = 50
        )

        // In seed, FEATURED is on Project A and Project C => expect exactly 2
        assertEquals(2, results.size, "Expected only FEATURED projects to be returned when mixed with unknown marker")
        assertTrue(results.all { it.markers.contains("FEATURED") }, "All returned projects must contain FEATURED marker")
    }
}
