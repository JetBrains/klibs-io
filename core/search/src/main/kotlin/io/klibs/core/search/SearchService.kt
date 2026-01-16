package io.klibs.core.search

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
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
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sort: SearchSort,
        markers: List<String>,
        tags: List<String>,
        page: Int,
        limit: Int
    ): List<SearchProjectResult> {
        return projectSearchRepository.find(
            query = query,
            platforms = platforms,
            targetFilters = targetFilters,
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
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sort: SearchSort,
        page: Int,
        limit: Int
    ): List<SearchPackageResult> {
        return packageSearchRepository.find(
            query = query,
            platforms = platforms,
            ownerLogin = ownerLogin,
            targetFilters = targetFilters,
            sortBy = sort,
            page = page,
            limit = limit
        )
    }


    private fun refreshProjectIndexView() {
        val refreshProjectsNanosTaken = measureNanoTime {
            projectSearchRepository.refreshIndex()
        }
        logger.info("Updated project search index in ${TimeUnit.NANOSECONDS.toSeconds(refreshProjectsNanosTaken)} seconds")
    }

    private fun refreshPackageIndexView() {
        val refreshPackagesNanosTaken = measureNanoTime {
            packageSearchRepository.refreshIndex()
        }
        logger.info("Updated package search index in ${TimeUnit.NANOSECONDS.toSeconds(refreshPackagesNanosTaken)} seconds")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SearchService::class.java)
    }
}
