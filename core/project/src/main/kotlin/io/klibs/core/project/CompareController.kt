package io.klibs.core.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/compare")
@Tag(name = "Compare", description = "Compare multiple projects side by side")
class CompareController(
    private val compareService: CompareService
) {
    @Operation(summary = "Compare multiple projects by owner login and project name")
    @PostMapping("/projects")
    fun compareProjects(
        @RequestBody request: CompareProjectRequest
    ): ResponseEntity<List<CompareProjectResponse?>> {
        if (request.projects.size > 10) {
            return ResponseEntity.badRequest().build()
        }
        if (request.projects.isEmpty()) {
            return ResponseEntity.ok(emptyList())
        }
        return ResponseEntity.ok(compareService.compareProjects(request.projects))
    }
}
