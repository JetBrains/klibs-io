package io.klibs.core.project.controller

import io.klibs.core.project.visibility.ProjectVisibilityChange
import io.klibs.core.project.visibility.ProjectVisibilityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/project-visibility")
@Tag(name = "Project visibility", description = "Operations for hiding and un-hiding projects")
class ProjectVisibilityController(
    private val projectVisibilityService: ProjectVisibilityService
) {
    @Operation(
        summary = "Stop serving a project",
        description = "The project keeps all of its data and keeps being indexed, it just leaves every read path. " +
                "A manual hide stays until it is un-hidden here, even if the SCM repository is reachable."
    )
    @PostMapping("/hide")
    fun hide(
        @RequestParam(name = "ownerLogin")
        @Parameter(description = "Login of the owner (same as the scm/github login)", example = "Kotlin")
        ownerLogin: String,

        @RequestParam(name = "projectName")
        @Parameter(description = "Name of the project (same as the scm/github repo name)", example = "kotlinx.coroutines")
        projectName: String,

        @RequestParam(name = "reason", required = false)
        @Parameter(description = "Reason for hiding the project", example = "Requested by the maintainer")
        reason: String?
    ): ResponseEntity<String> {
        val change = projectVisibilityService.hideManual(
            ownerLogin = ownerLogin,
            projectName = projectName,
            reason = reason?.takeIf { it.isNotBlank() }
        )

        return when (change) {
            ProjectVisibilityChange.CHANGED -> ResponseEntity.ok("Project $ownerLogin/$projectName is now hidden")
            ProjectVisibilityChange.ALREADY_IN_THAT_STATE ->
                ResponseEntity.ok("Project $ownerLogin/$projectName was already hidden")
            ProjectVisibilityChange.PROJECT_NOT_FOUND -> ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Serve a project again",
        description = "Un-hides the project whether it was hidden manually or by an unreachable SCM repository."
    )
    @PostMapping("/unhide")
    fun unhide(
        @RequestParam(name = "ownerLogin")
        @Parameter(description = "Login of the owner (same as the scm/github login)", example = "Kotlin")
        ownerLogin: String,

        @RequestParam(name = "projectName")
        @Parameter(description = "Name of the project (same as the scm/github repo name)", example = "kotlinx.coroutines")
        projectName: String
    ): ResponseEntity<String> {
        val change = projectVisibilityService.unhideManual(ownerLogin = ownerLogin, projectName = projectName)

        return when (change) {
            ProjectVisibilityChange.CHANGED -> ResponseEntity.ok("Project $ownerLogin/$projectName is served again")
            ProjectVisibilityChange.ALREADY_IN_THAT_STATE ->
                ResponseEntity.ok("Project $ownerLogin/$projectName was not hidden")
            ProjectVisibilityChange.PROJECT_NOT_FOUND -> ResponseEntity.notFound().build()
        }
    }
}
