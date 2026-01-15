package io.klibs.core.search

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup

interface PackageSearchRepository {
    /**
     * Search for packages based on the given criteria.
     *
     * @param query The search query to match against package fields
     * @param platforms Filter by supported platforms
     * @param ownerLogin Filter by owner login
     * @param sortBy Sort order
     * @param page Page number (1-based)
     * @param limit Maximum number of results per page
     * @return List of search results
     */
    fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        page: Int,
        limit: Int
    ): List<SearchPackageResult>

    /**
     * Refreshes the search index to include recently indexed / updated packages.
     */
    fun refreshIndex()
}