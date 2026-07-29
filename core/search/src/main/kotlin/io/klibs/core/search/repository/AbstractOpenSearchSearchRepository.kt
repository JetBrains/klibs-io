package io.klibs.core.search.repository

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.opensearch.OpenSearchQueryBuilder
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query

abstract class AbstractOpenSearchSearchRepository<T : Any>(
    private val client: OpenSearchClient,
) {

    protected abstract val indexName: String

    /** `_source` fields to fetch; must cover everything [toResult] reads. */
    protected abstract val sourceFields: List<String>

    protected abstract fun shouldClauses(query: String): List<Query>

    protected abstract fun sortOptions(sortBy: SearchSort, isQueryPresent: Boolean): List<SortOptions>

    protected abstract fun toResult(src: ObjectNode): T

    protected fun doFind(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        page: Int,
        limit: Int,
        extraFilters: List<Query> = emptyList(),
        popularityScored: Boolean = false,
    ): List<T> {
        val isQueryPresent = !query.isNullOrBlank()
        val trimmed = query?.trim().orEmpty()
        val filters = OpenSearchQueryBuilder.commonFilters(platforms, targetFilters, ownerLogin) + extraFilters
        val shoulds = if (isQueryPresent) shouldClauses(trimmed) else emptyList()
        val boolQuery = OpenSearchQueryBuilder.bool(shoulds, filters)
        val finalQuery = if (popularityScored && isQueryPresent && sortBy == SearchSort.RELEVANCY) {
            OpenSearchQueryBuilder.scored(boolQuery)
        } else {
            boolQuery
        }

        val response = client.search({ b ->
            b.index(indexName)
                .query(finalQuery)
                .from(limit * (page - 1))
                .size(limit)
                // return only used fields
                .source { s -> s.filter { f -> f.includes(sourceFields) } }
                .sort(sortOptions(sortBy, isQueryPresent))
        }, ObjectNode::class.java)

        return response.hits().hits().mapNotNull { it.source()?.let(::toResult) }
    }
}

internal fun fieldSort(field: String, order: SortOrder): SortOptions =
    SortOptions.of { it.field { f -> f.field(field).order(order) } }

internal fun scoreDesc(): SortOptions = SortOptions.of { it.score { s -> s.order(SortOrder.Desc) } }

internal fun ObjectNode.textOrNull(field: String): String? = get(field)?.takeUnless { it.isNull }?.asText()

internal fun ObjectNode.stringList(field: String): List<String> =
    get(field)?.takeIf { it.isArray }?.map { it.asText() } ?: emptyList()
