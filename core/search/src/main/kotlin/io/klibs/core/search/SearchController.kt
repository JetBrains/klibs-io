package io.klibs.core.search

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.search.dto.api.SearchPackageResultDTO
import io.klibs.core.search.dto.api.SearchPackageResultDTOTargetList
import io.klibs.core.search.dto.api.SearchPackagesRequest
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.suggest.SuggestService
import io.klibs.core.search.suggest.WordSuggestionDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Search projects, packages and owners")
@Validated
class SearchController(
    private val searchService: SearchService,
    private val suggestService: SuggestService,
) {
    @Operation(summary = "Search projects")
    @GetMapping("/projects")
    @Deprecated("This GET endpoint will be removed in future versions. Please use /projects endpoint with POST instead.")
    // TODO KTL-2556 when this will be removed, remove 'platforms' from SearchService and *SearchRepository methods too
    fun searchProjects(
        @RequestParam(required = false)
        @Parameter(
            description = "Arbitrary full text search query",
            example = "kotlinx"
        )
        query: String?,

        @RequestParam("platforms", required = false)
        @Parameter(
            description = "Filter by supported platforms. By default, this filter is not taken into account.",
            schema = Schema(
                type = "array",
                allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"],
            )
        )
        platforms: List<String> = emptyList(),

        @RequestParam("owner", required = false)
        @Parameter(
            description = "Login of the owner",
            schema = Schema(example = "Kotlin")
        )
        owner: String?,


        @RequestParam("sort", required = false, defaultValue = "relevance")
        @Parameter(
            description = "Sorting order",
            schema = Schema(allowableValues = ["most-stars", "relevance"], defaultValue = "relevance")
        )
        sortBy: String,

        @RequestParam("tags", required = false)
        @Parameter(
            description = "Filter by tags",
            schema = Schema(type = "array", example = "\"kotlin\", \"android\"")
        )
        tags: List<String> = emptyList(),

        @RequestParam("markers", required = false)
        @Parameter(
            description = "Filter by project markers",
            schema = Schema(type = "array", example = "\"FEATURED\", \"GRANT_WINNER_2024\"")
        )
        markers: List<String> = emptyList(),

        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", defaultValue = "1")
        )
        @Min(value = 0, message = "Page must be >= 0")
        page: Int,

        @RequestParam("limit", required = false, defaultValue = "20")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
        )
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 100, message = "Limit must be <= 100")
        limit: Int,
    ): List<SearchProjectResultDTO> {
        return searchService.search(
            query = query,
            platforms = platforms.map { PackagePlatform.findBySerializableName(it) },
            targetFilters = emptyMap(),
            ownerLogin = owner,
            sort = SearchSort.findBySerializableName(sortBy),
            markers = markers,
            tags = tags,
            page = page,
            limit = limit
        ).map { it.toDTO() }
    }



    @Operation(summary = "Search projects")
    @PostMapping("/projects")
    fun searchProjects(
        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", defaultValue = "1")
        )
        @Min(value = 0, message = "Page must be >= 0")
        page: Int,

        @RequestParam("limit", required = false, defaultValue = "20")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
        )
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 100, message = "Limit must be <= 100")
        limit: Int,

        @RequestBody
        @Parameter(description = "JSON body containing search parameters")
        @Valid
        searchRequest: SearchProjectsRequest
    ): List<SearchProjectResultDTO> {
        searchRequest.apply {
            val res = searchService.search(
                query = query,
                platforms = emptyList(),
                targetFilters = targetFilters,
                ownerLogin = owner,
                sort = SearchSort.findBySerializableName(sortBy),
                markers = markers,
                tags = tags,
                page = page,
                limit = limit
            )
            return res.map { it.toDTO() }
        }
    }

    @Operation(summary = "Search packages")
    @GetMapping("/packages")
    @Deprecated("This GET endpoint will be removed in future versions. Please use /packages endpoint with POST instead.")
    // TODO KTL-2556 note: when this will be removed, remove 'platforms' from SearchService and *SearchRepository methods too
    fun searchPackages(

        @RequestParam(required = false)
        @Parameter(
            description = "Arbitrary full text search query",
            example = "kotlinx"
        )
        query: String?,

        @RequestParam("platforms", required = false)
        @Parameter(
            description = "Filter by supported platforms. By default, this filter is not taken into account.",
            schema = Schema(
                type = "array",
                allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"],
            )
        )
        platforms: List<String> = emptyList(),

        @RequestParam("owner", required = false)
        @Parameter(
            description = "Login of the owner",
            schema = Schema(example = "Kotlin")
        )
        owner: String?,


        @RequestParam("sort", required = false, defaultValue = "relevance")
        @Parameter(
            description = "Sorting order",
            schema = Schema(allowableValues = ["most-stars", "relevance"], defaultValue = "relevance")
        )
        sortBy: String,

        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", defaultValue = "1")
        )
        @Min(value = 0, message = "Page must be >= 0")
        page: Int,

        @RequestParam("limit", required = false, defaultValue = "20")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
        )
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 100, message = "Limit must be <= 100")
        limit: Int,
    ): List<SearchPackageResultDTOTargetList> {
        return searchService.searchPackage(
            query = query,
            platforms = platforms.map { PackagePlatform.findBySerializableName(it) },
            targetFilters = emptyMap(),
            ownerLogin = owner,
            sort = SearchSort.findBySerializableName(sortBy),
            page = page,
            limit = limit
        ).map { it.toDTOTargetList() }
    }

    @Operation(summary = "Search packages")
    @PostMapping("/packages")
    fun searchPackages(
        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", defaultValue = "1")
        )
        @Min(value = 0, message = "Page must be >= 0")
        page: Int,

        @RequestParam("limit", required = false, defaultValue = "20")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")
        )
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 100, message = "Limit must be <= 100")
        limit: Int,

        @RequestBody
        @Parameter(description = "JSON body containing search parameters")
        @Valid
        searchRequest: SearchPackagesRequest
    ): List<SearchPackageResultDTO> {
        searchRequest.apply {
            val res = searchService.searchPackage(
                query = query,
                platforms = emptyList(),
                targetFilters = targetFilters,
                ownerLogin = owner,
                sort = SearchSort.findBySerializableName(sortBy),
                page = page,
                limit = limit
            )
            return res.map { it.toDTO() }
        }
    }

    @Operation(summary = "Suggest keywords")
    @GetMapping("/suggest")
    fun suggestKeywords(
        @RequestParam(required = false)
        @Parameter(
            description = "Arbitrary full text search query",
            example = "kotlinx"
        )
        query: String?,

        @RequestParam("limit", required = false, defaultValue = "5")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "5")
        )
        limit: Int
    ): WordSuggestionDTO {
        require(limit in 1..100) {
            "Limit must be in 1..100"
        }

        return WordSuggestionDTO(suggestService.suggestWords(query, limit))
    }
}

internal fun SearchProjectResult.toDTO(): SearchProjectResultDTO {
    return SearchProjectResultDTO(
        id = this.id,
        name = this.name,
        description = this.description,
        scmLink = "https://github.com/${this.ownerLogin}/${this.name}",
        scmStars = this.vcsStars,
        ownerType = this.ownerType.serializableName,
        ownerLogin = this.ownerLogin,
        licenseName = this.licenseName,
        latestReleaseVersion = this.latestVersion,
        latestReleasePublishedAtMillis = this.latestVersionPublishedAt.toEpochMilli(),
        platforms = this.platforms.map { it.serializableName },
        tags = this.tags,
        markers = markers
    )
}

internal fun SearchPackageResult.toDTO(): SearchPackageResultDTO {
    return SearchPackageResultDTO(
        groupId = this.groupId,
        artifactId = this.artifactId,
        description = this.description,
        scmLink = "https://github.com/${this.ownerLogin}/${this.artifactId}",
        ownerType = this.ownerType.serializableName,
        ownerLogin = this.ownerLogin,
        licenseName = this.licenseName,
        latestVersion = this.latestVersion,
        releaseTsMillis = this.releaseTs.toEpochMilli(),
        platforms = this.platforms.map { it.serializableName },
        targets = this.targetsMap
    )
}

internal fun SearchPackageResult.toDTOTargetList(): SearchPackageResultDTOTargetList {
    return SearchPackageResultDTOTargetList(
        groupId = this.groupId,
        artifactId = this.artifactId,
        description = this.description,
        scmLink = "https://github.com/${this.ownerLogin}/${this.artifactId}",
        ownerType = this.ownerType.serializableName,
        ownerLogin = this.ownerLogin,
        licenseName = this.licenseName,
        latestVersion = this.latestVersion,
        releaseTsMillis = this.releaseTs.toEpochMilli(),
        platforms = this.platforms.map { it.serializableName },
        targets = this.targetsList.map { "${it.platform}:${it.target}" },
    )
}

