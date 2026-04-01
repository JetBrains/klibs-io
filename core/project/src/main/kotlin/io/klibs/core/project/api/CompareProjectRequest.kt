package io.klibs.core.project.api

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CompareProjectRequest(
    @field:Size(
        min = 1,
        max = 10,
        message = "For comparison, you can only compare up to 10 projects at once and at least 1"
    )
    @field:Valid
    val projects: List<ProjectReference>
) {
    data class ProjectReference(
        @field:NotBlank("Owner login cannot be blank")
        val ownerLogin: String,
        @field:NotBlank("Project name cannot be blank")
        val projectName: String
    )
}