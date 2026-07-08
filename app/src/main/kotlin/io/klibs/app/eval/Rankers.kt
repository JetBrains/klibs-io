package io.klibs.app.eval

import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.service.SearchService

/** A search engine under evaluation: turns a [query] into a best-first ranking of projects. */
interface Ranker {
    val engineName: String
    fun rank(query: String, limit: Int): List<SearchProjectResult>
}

/** Baseline engine: the production PostgreSQL full-text search with relevance sorting. */
class FtsRanker(private val searchService: SearchService) : Ranker {
    override val engineName = "fts"

    override fun rank(query: String, limit: Int): List<SearchProjectResult> =
        searchService.search(
            query = query,
            platforms = emptyList(),
            targetFilters = emptyMap(),
            ownerLogin = null,
            sort = SearchSort.RELEVANCY,
            markers = emptyList(),
            tags = emptyList(),
            page = 1,
            limit = limit,
        )
}

/**
 * Semantic engine backed by a single embedding column. [engineName] must equal the embedder's
 * name so [SearchService.searchSimilarProjects] embeds the query with the matching model.
 */
class SemanticRanker(
    private val searchService: SearchService,
    override val engineName: String,
) : Ranker {
    override fun rank(query: String, limit: Int): List<SearchProjectResult> =
        searchService.searchSimilarProjects(
            query = query,
            embedderName = engineName,
            page = 1,
            limit = limit,
        )
}
