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
class CategoriesProjectsDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setup() {
        searchService.refreshSearchViews()
    }

    @Test
    @DisplayName("Each category returns its projects sorted by stars DESC")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `each category returns projects sorted by stars`() {
        val results = searchService.searchByCategories(limit = 10)

        val featured = results.entries.first { it.key.name == "Featured" }
        assertEquals(3, featured.value.size, "FEATURED has 3 projects (50001, 50002, 50006)")
        assertEquals(listOf(100, 90, 80), featured.value.map { it.vcsStars }, "Projects sorted by stars DESC")

        val compose = results.entries.first { it.key.name == "Compose UI" }
        assertEquals(2, compose.value.size, "COMPOSE_UI has 2 projects (50005, 50006)")
        assertEquals(listOf(90, 40), compose.value.map { it.vcsStars })
    }

    @Test
    @DisplayName("Multi-marker category (Grant winners) merges projects from both markers")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `multi-marker category merges markers`() {
        val results = searchService.searchByCategories(limit = 10)

        val grantWinners = results.entries.first { it.key.name == "Grant winners" }
        assertEquals(2, grantWinners.value.size, "Grant winners merges GRANT_WINNER_2023 + GRANT_WINNER_2024")
        assertEquals(listOf(60, 50), grantWinners.value.map { it.vcsStars }, "Sorted by stars DESC")
    }

    @Test
    @DisplayName("Limit caps the number of projects per category")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `limit is respected per category`() {
        val results = searchService.searchByCategories(limit = 2)

        val featured = results.entries.first { it.key.name == "Featured" }
        assertEquals(2, featured.value.size, "FEATURED capped at limit=2")
        assertEquals(listOf(100, 90), featured.value.map { it.vcsStars }, "Top-2 by stars")
    }

    @Test
    @DisplayName("Categories with no matching projects return empty list")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `empty categories return empty list`() {
        val results = searchService.searchByCategories(limit = 10)

        val networking = results.entries.first { it.key.name == "Networking" }
        assertTrue(networking.value.isEmpty(), "No projects have NETWORKING marker in seed data")
    }
}
