package io.klibs.core.search

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup

interface ProjectSearchRepository {

    fun findRandomByStars(minStars: Int, maxStars: Int, limit: Int): List<SearchProjectResult>

    /**
     * Good quality:
     * - Has a license
     * - Has a description
     * - Has a README
     *
     * @param searchLimit number of projects to select with a good quality
     * @param resultLimit how many random entries of [searchLimit] to return, to add "randomness"
     */
    fun findRecentlyCreatedWithGoodQuality(searchLimit: Int, resultLimit: Int): List<SearchProjectResult>

    fun find(
        query: String?,
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
        sortBy: SearchSort,
        tags: List<String>,
        markers: List<String>,
        page: Int,
        limit: Int
    ): List<SearchProjectResult>

    fun refreshIndex()
}
