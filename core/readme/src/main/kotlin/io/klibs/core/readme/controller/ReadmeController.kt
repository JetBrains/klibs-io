package io.klibs.core.project.controller

import io.klibs.core.readme.service.ReadmeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Readmes", description = "Fetching project readmes")
class ReadmeController(
    private val readmeService: ReadmeService
) {

    @Operation(summary = "Get a project's README in Markdown format")
    @GetMapping("/project/{ownerLogin}/{projectName}/readme/markdown", produces = ["text/plain"])
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
        return readmeService.getProjectReadmeMd(ownerLogin, projectName)
    }

    @Operation(summary = "Get a project's README in HTML format. Produced from Markdown via GitHub's API")
    @GetMapping("/project/{ownerLogin}/{projectName}/readme/html", produces = ["text/plain"])
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
        return readmeService.getProjectReadmeHtml(ownerLogin, projectName)
    }
}
