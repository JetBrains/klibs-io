package io.klibs.core.search.repository

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.opensearch.metrics.SearchQueryMetrics
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

/**
 * Serves project search from OpenSearch and degrades to PostgreSQL FTS when OpenSearch fails.
 */
@Primary
@Repository
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class ProjectSearchRepositoryFallback(
    private val openSearch: ProjectSearchRepositoryOpenSearch,
    private val postgres: ProjectSearchRepositoryJdbc,
    private val projectIndexSpec: OpenSearchIndexSpec,
    private val metrics: SearchQueryMetrics,
) : ProjectSearchRepository {

    override fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int,
    ): List<SearchProjectResult> = try {
        openSearch.find(query, platforms, targetGroupFilters, ownerLogin, sortBy, tags, markers, page, limit)
    } catch (e: UnsupportedOperationException) {
        // rethrow if deliberately skipped (i.e. oss health sort parameter)
        throw e
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        metrics.recordFallback(projectIndexSpec)
        log.warn("OpenSearch project search failed, falling back to PostgreSQL FTS (sort={})", sortBy, e)
        postgres.find(query, platforms, targetGroupFilters, ownerLogin, sortBy, tags, markers, page, limit)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(ProjectSearchRepositoryFallback::class.java)
    }
}
