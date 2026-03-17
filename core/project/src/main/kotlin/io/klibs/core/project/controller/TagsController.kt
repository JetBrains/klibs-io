package io.klibs.core.project.controller

import io.klibs.core.project.api.AllAllowedTagsResponse
import io.klibs.core.project.api.AllowedTagCreationRequest
import io.klibs.core.project.dto.AllowedTag
import io.klibs.core.project.mapper.AllowedTagMapper
import io.klibs.core.project.service.TagService
import io.klibs.core.project.tags.TagStatisticsDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tags")
@Tag(name = "Tags", description = "Information about project tags")
@Validated
class TagsController(
    private val tagService: TagService,
    private val allowedTagMapper: AllowedTagMapper
) {
    @Operation(summary = "Get tag statistics")
    @GetMapping("/stats")
    fun getTagStats(
        @RequestParam("limit", required = false, defaultValue = "20")
        @Parameter(
            description = "Max mount of tags to return",
            schema = Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "5")
        )
        limit: Int
    ): TagStatisticsDTO {
        return tagService.getTagStatistics(limit)
    }

    @Operation(summary = "Get all allowed tags")
    @GetMapping("/allowed")
    fun getAllAllowedTags(): AllAllowedTagsResponse {
        return allowedTagMapper.mapToAllAllowedTagsResponse(tagService.getAllowedProjectTags())
    }

    @Operation(summary = "Create allowed tag")
    @PostMapping("/allowed")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAllowedTag(@Valid @RequestBody request: AllowedTagCreationRequest): AllowedTag {
        return tagService.createAllowedTag(allowedTagMapper.mapAllowedTagCreationRequestToDto(request))
    }

    @Operation(summary = "Delete allowed tag")
    @DeleteMapping("/allowed/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAllowedTag(@PathVariable name: String) {
        tagService.deleteAllowedTag(name)
    }
}