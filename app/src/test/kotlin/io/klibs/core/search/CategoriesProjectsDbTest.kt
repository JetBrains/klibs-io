package io.klibs.core.search

import BaseUnitWithDbLayerTest
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.service.SearchService
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
class CategoriesProjectsDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: SearchService

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun setup() {
        uut.refreshSearchViews()
    }

    @Test
    @DisplayName("Each category returns its projects sorted by stars DESC")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `each category returns projects sorted by stars`() {
        val results = uut.searchByCategories(page = 1, limit = 10)

        val grantWinnersCategory = results.first { it.categoryName == "Grant winners" }
        assertEquals(3, grantWinnersCategory.projects.size, "Grant winners has 3 projects (50003, 50004, 50007)")
        assertEquals(
            listOf(60, 50, 45),
            grantWinnersCategory.projects.map { it.vcsStars },
            "Projects sorted by stars DESC"
        )

        val compose = results.first { it.categoryName == "Compose UI" }
        assertEquals(2, compose.projects.size, "COMPOSE_UI has 2 projects (50005, 50006)")
        assertEquals(listOf(90, 40), compose.projects.map { it.vcsStars })
    }

    @Test
    @DisplayName("Multi-marker category (Grant winners) merges projects from both markers")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `multi-marker category merges markers`() {
        val results = uut.searchByCategories(page = 1, limit = 10)

        val grantWinners = results.first { it.categoryName == "Grant winners" }
        assertEquals(
            3,
            grantWinners.projects.size,
            "Grant winners merges GRANT_WINNER_2023 + GRANT_WINNER_2024 + GRANT_WINNER_2025"
        )
        assertEquals(listOf(60, 50, 45), grantWinners.projects.map { it.vcsStars }, "Sorted by stars DESC")
    }

    @Test
    @DisplayName("Limit caps the number of projects per category")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `limit is respected per category`() {
        val results = uut.searchByCategories(page = 1, limit = 2)

        val grantWinnersCategory = results.first { it.categoryName == "Grant winners" }
        assertEquals(2, grantWinnersCategory.projects.size, "Grant winners capped at limit=2")
        assertEquals(listOf(60, 50), grantWinnersCategory.projects.map { it.vcsStars }, "Top-2 by stars")
    }

    @Test
    @DisplayName("Categories with no matching projects return empty list")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `empty categories return empty list`() {
        val results = uut.searchByCategories(page = 1, limit = 10)

        val networking = results.first { it.categoryName == "Networking" }
        assertTrue(networking.projects.isEmpty(), "No projects have NETWORKING marker in seed data")
    }

    @Test
    @DisplayName("Category preview rejects a non-positive limit")
    fun `category preview rejects a non positive limit`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            uut.searchByCategories(page = 1, limit = 0)
        }

        assertEquals("Limit must be >= 1", exception.message)
    }

    @Test
    @DisplayName("Categories preserve their database order")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `categories preserve their database order`() {
        val categoryNames = uut.searchByCategories(page = 1, limit = 10).map { it.categoryName }

        assertEquals(listOf("JetBrains and Google", "Compose UI"), categoryNames.take(2))
    }

    @Test
    @DisplayName("JetBrains and Google exposes its marker and subtitle")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category exposes selector metadata`() {
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "JetBrains and Google" }

        assertEquals(listOf("JetBrains and Google"), category.categoryMarkers)
        assertEquals("Essential libraries from the JetBrains and Google ecosystem", category.subtitle)
    }

    @Test
    @DisplayName("JetBrains and Google includes exact owner logins ordered by dependent count")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category includes exact owners ordered by dependent count`() {
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "JetBrains and Google" }

        assertEquals(listOf(50008, 50010, 50011, 50013, 50009, 50012), category.projects.map { it.id })
        assertEquals(
            listOf("Kotlin", "androidx", "JetBrains", "JetBrains", "Kotlin", "androidx"),
            category.projects.map { it.ownerLogin },
        )
    }

    @Test
    @DisplayName("Owner-based category membership is independent of project markers")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category includes projects that have unrelated markers`() {
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "JetBrains and Google" }

        assertTrue(
            category.projects.any { it.id == 50008 && "CATEGORY_SELECTOR_TEST" in it.markers },
            "Expected project 50008 with marker CATEGORY_SELECTOR_TEST in category 'JetBrains and Google', " +
                    "but found ${category.projects.map { it.id to it.markers }}",
        )
    }

    @Test
    @DisplayName("Hidden projects are excluded from owner-based categories")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category excludes hidden projects`() {
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "JetBrains and Google" }

        assertFalse(
            category.projects.any { it.id == 50017 },
            "Expected hidden project 50017 to be absent from category results, but found ${category.projects.map { it.id }}",
        )
    }

    @Test
    @DisplayName("Owner-based category preview matches its first pageable search results")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category preview matches the first page`() {
        val preview = uut.searchByCategories(page = 1, limit = 3)
            .single { it.categoryName == "JetBrains and Google" }
            .projects.map { it.id }

        assertEquals(listOf(50008, 50010, 50011), preview)
        assertEquals(preview, searchCategory(limit = 3).map { it.id })
    }

    @Test
    @DisplayName("Owner-based category pagination returns the expected stable page order")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category pagination returns stable page order`() {
        val pages = (1..4).map { page -> searchCategory(page = page, limit = 2) }

        assertEquals(listOf(50008, 50010), pages[0].map { it.id })
        assertEquals(listOf(50011, 50013), pages[1].map { it.id })
        assertEquals(listOf(50009, 50012), pages[2].map { it.id })
        assertTrue(pages[3].isEmpty(), "Expected page 4 (limit=2) to be empty, but got ${pages[3].map { it.id }}")
        val repeatedPages = (1..4).map { page -> searchCategory(page = page, limit = 2) }
        assertEquals(
            pages.map { it.map { project -> project.id } },
            repeatedPages.map { it.map { project -> project.id } })
    }

    @Test
    @DisplayName("Owner-based category pagination is exhaustive and duplicate-free")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category pagination is exhaustive and duplicate free`() {
        val allProjects = (1..4).flatMap { page -> searchCategory(page = page, limit = 2) }.map { it.id }

        assertEquals(listOf(50008, 50010, 50011, 50013, 50009, 50012), allProjects)
        assertEquals(6, allProjects.toSet().size)
    }

    @Test
    @DisplayName("JetBrains owner category query returns only JetBrains-owned projects by dependents")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search for JetBrains returns JetBrains projects`() {
        val results = searchCategory(query = "JetBrains", limit = 10)

        assertEquals(listOf(50011, 50013), results.map { it.id })
        assertTrue(results.all { it.ownerLogin == "JetBrains" })
    }

    @Test
    @DisplayName("Kotlin owner category query returns only Kotlin-owned projects by dependents")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search for Kotlin returns Kotlin projects`() {
        val results = searchCategory(query = "Kotlin", limit = 10)

        assertEquals(listOf(50008, 50009), results.map { it.id })
        assertTrue(results.all { it.ownerLogin == "Kotlin" })
    }

    @Test
    @DisplayName("Owner-based category search applies the target filter")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search filters by target`() {
        val results = searchCategory(
            targetGroupFilters = listOf(mapOf(TargetGroup.JVM to setOf("17"))),
            limit = 10,
        )

        assertEquals(listOf(50008), results.map { it.id })
    }

    @Test
    @DisplayName("Owner-based category search applies the additional owner filter")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search filters by owner login`() {
        val results = searchCategory(ownerLogin = "Kotlin", limit = 10)

        assertEquals(listOf(50008, 50009), results.map { it.id })
    }

    @Test
    @DisplayName("Owner-based category search applies the tag filter")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search filters by tag`() {
        val results = searchCategory(tags = listOf("category-test-tag"), limit = 10)

        assertEquals(listOf(50008), results.map { it.id })
    }

    @Test
    @DisplayName("Owner marker combines with other markers using OR")
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `JetBrains and Google category search filters by marker`() {
        val results = searchCategory(markers = listOf("JetBrains and Google", "CATEGORY_SELECTOR_TEST"), limit = 10)

        assertTrue(results.map { it.id }.containsAll(listOf(50008, 50010, 50011, 50013, 50009, 50012)))
    }

    @Test
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `marker category search uses its marker filters`() {
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "Grant winners" }
        assertEquals(
            category.projects.map { it.id },
            searchCategory(
                limit = 10, markers = category.categoryMarkers,
                sort = SearchSort.MOST_STARS
            ).map { it.id },
        )
    }


    @Test
    @Transactional
    @Sql(value = ["classpath:sql/CategoriesProjectsDbTest/seed.sql"])
    fun `preview and search combine owner marker and regular markers`() {
        jdbcClient.sql(
            """
            UPDATE category SET markers = ARRAY['JetBrains and Google', 'GRANT_WINNER_2024']
            WHERE name = 'JetBrains and Google'
        """
        ).update()
        val category = uut.searchByCategories(page = 1, limit = 10).single { it.categoryName == "JetBrains and Google" }
        assertTrue(category.projects.any { it.ownerLogin !in listOf("Kotlin", "JetBrains", "androidx") })
        assertEquals(
            category.projects.map { it.id },
            searchCategory(limit = 10, markers = category.categoryMarkers).map { it.id },
        )
    }

    private fun searchCategory(
        query: String? = null,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>> = emptyList(),
        ownerLogin: String? = null,
        tags: List<String> = emptyList(),
        markers: List<String> = listOf("JetBrains and Google"),
        page: Int = 1,
        limit: Int,
        sort: SearchSort = SearchSort.MOST_DEPENDENTS,
    ) = uut.search(
        query = query,
        targetGroupFilters = targetGroupFilters,
        ownerLogin = ownerLogin,
        sort = sort,
        markers = markers,
        tags = tags,
        page = page,
        limit = limit,
    )
}
