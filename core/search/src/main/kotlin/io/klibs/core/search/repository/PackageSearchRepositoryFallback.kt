package io.klibs.core.search.repository

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.opensearch.metrics.SearchQueryMetrics
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

/** Serves package search from OpenSearch and degrades to PostgreSQL FTS when OpenSearch fails. */
@Primary
@Repository
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class PackageSearchRepositoryFallback(
    private val openSearch: PackageSearchRepositoryOpenSearch,
    private val postgres: PackageSearchRepositoryJdbc,
    private val packageIndexSpec: OpenSearchIndexSpec,
    private val metrics: SearchQueryMetrics,
) : PackageSearchRepository {

    override fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        page: Int,
        limit: Int,
    ): List<SearchPackageResult> = try {
        openSearch.find(query, platforms, targetFilters, ownerLogin, sortBy, page, limit)
    } catch (e: UnsupportedOperationException) {
        throw e
    } catch (e: Exception) {
        metrics.recordFallback(packageIndexSpec)
        log.warn("OpenSearch package search failed, falling back to PostgreSQL FTS (sort={})", sortBy, e)
        postgres.find(query, platforms, targetFilters, ownerLogin, sortBy, page, limit)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PackageSearchRepositoryFallback::class.java)
    }
}
