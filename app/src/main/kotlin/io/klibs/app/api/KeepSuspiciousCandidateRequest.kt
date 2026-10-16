package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Resolves every PENDING row of (projectId, groupId) without banning anything")
data class KeepSuspiciousCandidateRequest(
    val projectId: Int,
    @field:NotBlank(message = "Group ID cannot be blank")
    val groupId: String,
    @Schema(description = "Stored as the candidate notes")
    @field:NotBlank(message = "Note cannot be blank")
    @field:Size(max = 255, message = "Note must be at most 255 characters")
    val note: String,
)
