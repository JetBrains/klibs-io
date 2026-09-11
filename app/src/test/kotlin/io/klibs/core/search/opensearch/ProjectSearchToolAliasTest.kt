package io.klibs.core.search.opensearch

import BaseOpenSearchTest
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.repository.ProjectSearchRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Curated tool aliases (KTL-4925): searching for a tool klibs.io does not host must lead to its KMP
 * equivalent, without changing what plain queries return.
 */
class ProjectSearchToolAliasTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var indexer: OpenSearchIndexer

    @Autowired
    @Qualifier("projectIndexSpec")
    private lateinit var projectSpec: OpenSearchIndexSpec

    @Autowired
    private lateinit var repository: ProjectSearchRepository

    @Test
    @Sql(value = [SEED])
    fun `a tool name resolves to its KMP equivalent`() {
        indexer.sync(projectSpec)

        assertTrue(search("Hilt").contains("koin"), "Hilt must lead to koin")
        assertTrue(search("Dagger").contains("koin"), "Dagger must lead to koin")
    }

    @Test
    @Sql(value = [SEED])
    fun `a multi-word alias fires only on the full phrase`() {
        indexer.sync(projectSpec)

        // `room` alone still means androidx/room; only "room alternative" asks for a replacement.
        assertEquals("room", search("room").first())
        assertFalse(search("room").contains("sqldelight"), "a plain `room` query must not pull in sqldelight")
        assertTrue(search("room alternative").contains("sqldelight"))
    }

    @Test
    @Sql(value = [SEED])
    fun `a query naming no tool is unaffected by the alias clause`() {
        indexer.sync(projectSpec)

        assertTrue(search("koin").contains("koin"))
        assertTrue(search("persistence").contains("room"))
    }

    private fun search(query: String): List<String> = repository.find(
        query = query,
        platforms = emptyList(),
        targetGroupFilters = emptyList(),
        ownerLogin = null,
        sortBy = SearchSort.RELEVANCY,
        tags = emptyList(),
        markers = emptyList(),
        page = 1,
        limit = 10,
    ).map { it.name }

    private companion object {
        private const val SEED = "classpath:sql/ProjectSearchToolAliasTest/seed.sql"
    }
}
