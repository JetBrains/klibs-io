package io.klibs.core.project.tags

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tags")
@Tag(name = "Tags", description = "Information about project tags")
class TagsController(
    private val tagService: TagService
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
}