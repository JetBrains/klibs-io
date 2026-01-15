package io.klibs.core.project.tags

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "TagStatistics",
    description = "Statistics about tags and their projects count"
)
data class TagStatisticsDTO (
    @Schema(
        description = "Total number of projects known",
        example = "40"
    )
    val totalProjectsCount: Long,

    @Schema(
        description = "List of tags with their projects count",
        example = "[{\"tag\":\"kotlin\",\"projectsCount\":100}]"
    )
    val tags: List<TagData>
)

@Schema(
    name = "TagData",
    description = "Data about a tag and its projects count"
)
data class TagData(
    val tag: String,
    val projectsCount: Long
)