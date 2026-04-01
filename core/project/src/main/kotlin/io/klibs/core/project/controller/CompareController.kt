package io.klibs.core.project.controller

import io.klibs.core.project.api.CompareProjectRequest
import io.klibs.core.project.api.CompareProjectResponse
import io.klibs.core.project.mapper.CompareProjectMapper
import io.klibs.core.project.service.CompareProjectService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/compare")
@Tag(name = "Compare", description = "Compare multiple projects side by side")
class CompareController(
    private val compareProjectService: CompareProjectService,
    private val compareProjectMapper: CompareProjectMapper,
) {
    @Operation(summary = "Compare multiple projects by owner login and project name")
    @PostMapping("/projects")
    fun compareProjects(
        @Valid @RequestBody request: CompareProjectRequest
    ): List<CompareProjectResponse> {
        return compareProjectService.compareProjects(request.projects)
            .map { (details, kotlinVersion) -> compareProjectMapper.mapToCompareResponse(details, kotlinVersion) }
    }
}
