package io.klibs.core.search.service

import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.repository.PackageSearchRepository
import io.klibs.core.search.repository.PackageSearchRepositoryJdbc
import io.klibs.core.search.repository.ProjectSearchRepository
import io.klibs.core.search.repository.ProjectSearchRepositoryJdbc
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.dto.service.CategoryWithProjects
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKERS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_MARKER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_OWNER_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_QUERY_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAGS
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TAG_LENGTH
import io.klibs.core.search.dto.validation.SearchLimits.MAX_TARGET_GROUP_FILTERS
import io.klibs.core.search.dto.validation.ValidTargetGroupValues
import io.micrometer.core.annotation.Timed
import jakarta.validation.constraints.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

@Service
@Validated
class SearchService(
    private val projectSearchRepository: ProjectSearchRepository,
    private val packageSearchRepository: PackageSearchRepository,
    private val projectIndexJdbc: ProjectSearchRepositoryJdbc,
    private val packageIndexJdbc: PackageSearchRepositoryJdbc,
    private val applicationScope: CoroutineScope
) {
    /**
     * Refreshes the project search indexes to include recently indexed / updated packages and projects.
     */
    fun refreshSearchViews() {
        try {
            refreshPackageIndexView()
            refreshProjectIndexView()
        } catch (e: Exception) {
            logger.error("Unable to refresh search views", e)
        }
    }

    /**
     * Asynchronously refreshes the search views.
     */
    fun refreshSearchViewsAsync() {
        applicationScope.launch(Dispatchers.IO) {
            refreshSearchViews()
        }
    }

    @Transactional(readOnly = true)
    @Timed(value = "klibs.project.search.time", description = "Klibs: Time taken to search projects")
    fun search(
        @Size(max = MAX_QUERY_LENGTH) query: String?,
        @Size(max = MAX_TARGET_GROUP_FILTERS) @ValidTargetGroupValues
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        @Size(max = MAX_OWNER_LENGTH) ownerLogin: String?,
        sort: SearchSort,
        @Size(max = MAX_MARKERS) markers: List<@Size(max = MAX_MARKER_LENGTH) String>,
        @Size(max = MAX_TAGS) tags: List<@Size(max = MAX_TAG_LENGTH) String>,
        page: Int,
        limit: Int
    ): List<SearchProjectResult> {
        return projectSearchRepository.find(
            query = query,
            targetGroupFilters = targetGroupFilters,
            ownerLogin = ownerLogin,
            sortBy = sort,
            markers = markers,
            tags = tags,
            page = page,
            limit = limit
        )
    }

    @Transactional(readOnly = true)
    @Timed(value = "klibs.package.search.time", description = "Klibs: Time taken to search packages")
    fun searchPackage(
        @Size(max = MAX_QUERY_LENGTH) query: String?,
        @Size(max = MAX_TARGET_GROUP_FILTERS) @ValidTargetGroupValues
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        @Size(max = MAX_OWNER_LENGTH) ownerLogin: String?,
        sort: SearchSort,
        page: Int,
        limit: Int
    ): List<SearchPackageResult> {
        return packageSearchRepository.find(
            query = query,
            ownerLogin = ownerLogin,
            targetGroupFilters = targetGroupFilters,
            sortBy = sort,
            page = page,
            limit = limit
        )
    }


    @Transactional(readOnly = true)
    fun searchByCategories(limit: Int): List<CategoryWithProjects> {
        return projectIndexJdbc.findCategoriesWithProjects(limit)
            .map { (category, projects) ->
                CategoryWithProjects(
                    categoryName = category.name,
                    subtitle = category.subtitle,
                    categoryMarkers = category.markers,
                    projects = projects,
                )
            }
    }

    private fun refreshProjectIndexView() {
        val refreshProjectsNanosTaken = measureNanoTime { projectIndexJdbc.refreshIndex() }
        logger.info("Updated project search index in ${TimeUnit.NANOSECONDS.toSeconds(refreshProjectsNanosTaken)} seconds")
    }

    private fun refreshPackageIndexView() {
        val refreshPackagesNanosTaken = measureNanoTime { packageIndexJdbc.refreshIndex() }
        logger.info("Updated package search index in ${TimeUnit.NANOSECONDS.toSeconds(refreshPackagesNanosTaken)} seconds")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SearchService::class.java)
    }
}