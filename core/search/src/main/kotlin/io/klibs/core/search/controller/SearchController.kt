package io.klibs.core.search.controller

import io.klibs.core.search.dto.api.SearchPackageResultDTO
import io.klibs.core.search.dto.api.SearchPackagesRequest
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import io.klibs.core.search.dto.api.SearchProjectsRequest
import io.klibs.core.search.dto.api.toDTO
import io.klibs.core.search.dto.repository.SearchPackageResult
import io.klibs.core.search.service.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
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
) {
    @Operation(summary = "Search projects")
    @PostMapping("/projects")
    fun searchProjects(
        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "1")
        )
        @Min(value = 1, message = "Page must be >= 1")
        @Max(value = 50, message = "Page must be <= 50")
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
                targetGroupFilters = targetGroupFilters,
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
    @PostMapping("/packages")
    fun searchPackages(
        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "1")
        )
        @Min(value = 1, message = "Page must be >= 1")
        @Max(value = 50, message = "Page must be <= 50")
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
                targetGroupFilters = targetGroupFilters,
                ownerLogin = owner,
                sort = SearchSort.findBySerializableName(sortBy),
                page = page,
                limit = limit
            )
            return res.map { it.toDTO() }
        }
    }
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
        targets = this.targetGroups,
        targetGroups = this.targetGroups,
    )
}
