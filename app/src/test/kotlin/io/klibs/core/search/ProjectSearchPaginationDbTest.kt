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
class ProjectSearchPaginationDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var projectSearchRepository: ProjectSearchRepositoryJdbc

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Paginated results should not contain duplicates when projects share the same star count")
    @Sql(value = ["classpath:sql/ProjectSearchPaginationDbTest/seed.sql"])
    fun `no duplicates across pages when stars are equal`() {
        val pageSize = 3

        val page1 = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.MOST_STARS,
            tags = emptyList(),
            markers = emptyList(),
            page = 1,
            limit = pageSize
        )

        val page2 = projectSearchRepository.find(
            rawQuery = null,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sortBy = SearchSort.MOST_STARS,
            tags = emptyList(),
            markers = emptyList(),
            page = 2,
            limit = pageSize
        )

        assertEquals(pageSize, page1.size, "Page 1 should contain $pageSize results")
        assertEquals(pageSize, page2.size, "Page 2 should contain $pageSize results")

        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        val overlap = page1Ids.intersect(page2Ids)

        assertTrue(overlap.isEmpty(), "Pages must not share any project IDs, but found duplicates: $overlap")

        val allIds = page1Ids + page2Ids
        assertEquals(6, allIds.size, "All 6 seeded projects should appear across the two pages")
    }
}
