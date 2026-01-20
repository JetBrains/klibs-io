package io.klibs.core.project

import io.klibs.core.pckg.api.PackageOverviewResponse
import io.klibs.core.pckg.controller.toDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/project")
@Tag(name = "Projects", description = "Information about projects")
class ProjectController(
    private val projectService: ProjectService
) {
    @Operation(summary = "Get full project information by owner login and project name")
    @GetMapping("/{ownerLogin}/{projectName}/details")
    fun getProjectDetailsByName(
        @PathVariable
        @Parameter(
            description = "Login of the owner (same as the scm/github login)",
            example = "Kotlin"
        )
        ownerLogin: String,

        @PathVariable
        @Parameter(
            description = "Name of the project (same as the scm/github repo name)",
            example = "kotlinx.coroutines"
        )
        projectName: String
    ): ResponseEntity<ProjectDetailsDTO> {
        val projectDetails = projectService.getProjectDetailsByName(
            ownerLogin = ownerLogin,
            projectName = projectName
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(projectDetails.toDTO())
    }

    @Operation(summary = "Get full project information by its id")
    @GetMapping("/{projectId}/details")
    fun getProjectDetailsById(
        @PathVariable
        @Parameter(
            description = "ID of the project, such as when it's returned by package endpoints",
            example = "10"
        )
        projectId: Int,
    ): ProjectDetailsDTO? {
        return projectService.getProjectDetailsById(projectId)?.toDTO()
    }

    @Operation(summary = "Get an overview of a project's packages")
    @GetMapping("/{ownerLogin}/{projectName}/packages")
    fun getProjectPackages(
        @PathVariable
        @Parameter(
            description = "Login of the owner (same as the scm/github login)",
            example = "Kotlin"
        )
        ownerLogin: String,

        @PathVariable
        @Parameter(
            description = "Name of the project (same as the scm/github repo name)",
            example = "kotlinx.coroutines"
        )
        projectName: String
    ): List<PackageOverviewResponse> {
        return projectService.getLatestProjectPackages(
            ownerLogin = ownerLogin,
            projectName = projectName
        ).map { it.toDTO() }
    }

    @Operation(summary = "Get a project's README in Markdown format")
    @GetMapping("/{ownerLogin}/{projectName}/readme/markdown", produces = ["text/plain"])
    fun getProjectReadmeMarkdown(
        @PathVariable
        @Parameter(
            description = "Login of the owner (same as the scm/github login)",
            example = "Kotlin"
        )
        ownerLogin: String,

        @PathVariable
        @Parameter(
            description = "Name of the project (same as the scm/github repo name)",
            example = "kotlinx.coroutines"
        )
        projectName: String
    ): String? {
        return projectService.getProjectReadmeMd(ownerLogin, projectName)
    }

    @Operation(summary = "Get a project's README in HTML format. Produced from Markdown via GitHub's API")
    @GetMapping("/{ownerLogin}/{projectName}/readme/html", produces = ["text/plain"])
    fun getProjectReadmeHtml(
        @PathVariable
        @Parameter(
            description = "Login of the owner (same as the scm/github login)",
            example = "Kotlin"
        )
        ownerLogin: String,

        @PathVariable
        @Parameter(
            description = "Name of the project (same as the scm/github repo name)",
            example = "kotlinx.coroutines"
        )
        projectName: String
    ): String? {
        return projectService.getProjectReadmeHtml(ownerLogin, projectName)
    }

}


private fun ProjectDetails.toDTO(): ProjectDetailsDTO {
    return ProjectDetailsDTO(
        id = this.id,
        ownerType = this.ownerType.serializableName,
        ownerLogin = this.ownerLogin,
        name = this.name,
        description = this.description,
        platforms = this.platforms.map { it.serializableName },
        latestReleaseVersion = this.latestReleaseVersion,
        latestReleasePublishedAtMillis = this.latestReleasePublishedAt?.toEpochMilli(),
        linkHomepage = this.linkHomepage,
        linkScm = this.getGitHubRepositoryLink(),
        linkGitHubPages = this.getGitHubPagesLink(),
        linkIssues = this.getIssuesLink(),
        linkWiki = this.getWikiLink(),
        scmStars = this.stars,
        createdAtMillis = this.createdAt.toEpochMilli(),
        openIssues = this.openIssues,
        licenseName = this.licenseName,
        lastActivityAtMillis = this.lastActivityAt.toEpochMilli(),
        updatedAtMillis = this.updatedAt.toEpochMilli(),
        tags = tags,
        markers = markers
    )
}
