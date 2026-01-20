package io.klibs.app.controller

import io.klibs.app.api.UpdatePackageDescriptionRequest
import io.klibs.app.api.UpdateProjectDescriptionRequest
import io.klibs.app.api.UpdateProjectTagsRequest
import io.klibs.core.pckg.service.PackageDescriptionService
import io.klibs.core.project.ProjectService
import io.klibs.core.search.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/content")
@Tag(name = "Content Update", description = "Endpoints for updating klibs.io content")
@Validated
class ContentUpdateController(
    private val projectService: ProjectService,
    private val packageDescriptionService: PackageDescriptionService,
    private val searchService: SearchService
) {
    @Operation(summary = "Update project's description")
    @PatchMapping("/project/description")
    fun updateProjectDescription(
        @Valid @RequestBody
        request: UpdateProjectDescriptionRequest
    ): ResponseEntity<Void> {
        projectService.updateProjectDescription(request.projectName, request.ownerLogin, request.description)
        searchService.refreshSearchViewsAsync()
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Update project's tags")
    @PatchMapping("/project/tags")
    fun updateProjectTags(
        @Valid @RequestBody
        request: UpdateProjectTagsRequest
    ): ResponseEntity<List<String>> {
        val updated = projectService.updateUserTags(request.projectName, request.ownerLogin, request.tags)
        searchService.refreshSearchViewsAsync()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Update a package description directly",
        description = "Updates the description of a package identified by groupId, artifactId, and version with a user-provided description"
    )
    @PatchMapping("/package/description")
    fun updatePackageDescription(
        @Valid @RequestBody request: UpdatePackageDescriptionRequest
    ): String {
        val description = packageDescriptionService.updatePackageDescription(
            request.groupId,
            request.artifactId,
            request.version,
            request.description
        )
        searchService.refreshSearchViewsAsync()
        return description
    }
}
