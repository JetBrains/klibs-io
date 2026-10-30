package io.klibs.core.search.service

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import io.klibs.core.search.repository.PackageSearchRepository
import io.klibs.core.search.repository.PackageSearchRepositoryJdbc
import io.klibs.core.search.repository.ProjectSearchRepository
import io.klibs.core.search.repository.ProjectSearchRepositoryJdbc
import io.klibs.core.search.enums.CategoryProjectSelector
import io.klibs.core.search.controller.SearchSort
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.dto.repository.SearchProjectResult
import io.klibs.core.search.dto.service.CategoryWithProjects
import io.klibs.core.search.dto.validation.validateTargetGroupFilters
import io.micrometer.core.annotation.Timed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

@Service
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
        query: String?,
        platforms: List<PackagePlatform>,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
        sort: SearchSort,
        markers: List<String>,
        tags: List<String>,
        page: Int,
        limit: Int,
        categorySelector: CategoryProjectSelector? = null,
    ): List<SearchProjectResult> {
        validatePagination(page, limit)
        validateTargetGroupFilters(targetGroupFilters)?.let { throw IllegalArgumentException(it) }

        return projectSearchRepository.find(
            query = query,
            platforms = platforms,
            targetGroupFilters = targetGroupFilters,
            ownerLogin = ownerLogin,
            sortBy = if (categorySelector != null) SearchSort.MOST_DEPENDENTS else sort,
            markers = markers,
            tags = tags,
            page = page,
            limit = limit,
            ownerLogins = categorySelector?.ownerLogins,
        )
    }

    @Transactional(readOnly = true)
    @Timed(value = "klibs.package.search.time", description = "Klibs: Time taken to search packages")
    fun searchPackage(
        query: String?,
        platforms: List<PackagePlatform>,
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
        sort: SearchSort,
        page: Int,
        limit: Int
    ): List<SearchPackageResult> {
        validatePagination(page, limit)
        validateTargetGroupFilters(targetGroupFilters)?.let { throw IllegalArgumentException(it) }
        return packageSearchRepository.find(
            query = query,
            platforms = platforms,
            ownerLogin = ownerLogin,
            targetGroupFilters = targetGroupFilters,
            sortBy = sort,
            page = page,
            limit = limit
        )
    }


    @Transactional(readOnly = true)
    fun searchByCategories(limit: Int): List<CategoryWithProjects> {
        validatePagination(page = 1, limit = limit)
        val ownerCategories = CategoryProjectSelector.entries.associate { it.categoryName to it.ownerLogins }
        return projectIndexJdbc.findCategoriesWithProjects(limit, ownerCategories)
            .map { (category, projects) ->
                val selector = CategoryProjectSelector.forCategoryName(category.name)

                CategoryWithProjects(
                    categoryName = category.name,
                    categoryMarkers = category.markers,
                    selector = selector,
                    projects = projects,
                )
            }
    }

    private fun validatePagination(page: Int, limit: Int) {
        if (page < 1 || limit < 1) {
            logger.warn("Discarding search request: page={}, limit={}", page, limit)
        }
        require(page >= 1) { "Page must be >= 1" }
        require(limit >= 1) { "Limit must be >= 1" }
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