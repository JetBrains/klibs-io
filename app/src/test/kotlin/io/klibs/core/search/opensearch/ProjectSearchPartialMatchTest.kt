package io.klibs.core.search.opensearch

import BaseOpenSearchTest
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.repository.ProjectSearchRepositoryOpenSearch
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertTrue

/**
 * Partial-name search: part of a library name has to find the library, whether the user
 * typed its head or its tail, and whichever clause ends up serving the query.
 *
 * Queries the OpenSearch repository directly: the @Primary fallback answers from PostgreSQL FTS on
 * any OpenSearch failure, which would turn a broken clause into a passing test.
 */
class ProjectSearchPartialMatchTest : BaseOpenSearchTest() {

    @Autowired
    private lateinit var indexer: OpenSearchIndexer

    @Autowired
    @Qualifier("projectIndexSpec")
    private lateinit var projectSpec: OpenSearchIndexSpec

    @Autowired
    private lateinit var repository: ProjectSearchRepositoryOpenSearch

    @Test
    @Sql(value = [SEED])
    fun `the head of a name finds the library`() {
        indexer.sync(projectSpec)

        assertTrue(search("sqldel").contains("sqldelight"), "`sqldel` must find sqldelight")
    }

    @Test
    @Sql(value = [SEED])
    fun `the tail of a name finds the library`() {
        indexer.sync(projectSpec)

        assertTrue(search("delight").contains("sqldelight"), "`delight` must find sqldelight")
    }

    @Test
    @Sql(value = [SEED])
    fun `a partial artifact id finds the library`() {
        indexer.sync(projectSpec)

        assertTrue(search("koin cor").contains("koin"), "`koin cor` must find koin-core")
    }

    @Test
    @Sql(value = [SEED])
    fun `a head longer than the indexed ngrams still finds the library`() {
        indexer.sync(projectSpec)

        // 19 chars: one past max_gram, so no indexed gram can serve it, and 11 chars short of the
        // full name, so fuzzy (edit distance 2) cannot bridge it either.
        assertTrue(
            search("multiplatformsettin").contains(LONG_NAME),
            "a head longer than max_gram must still match",
        )
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
        private const val SEED = "classpath:sql/ProjectSearchPartialMatchTest/seed.sql"
        private const val LONG_NAME = "MultiplatformSettingsDataStore"
    }
}
