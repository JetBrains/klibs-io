package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Bans one artifact globally and resolves its PENDING row")
data class BanSuspiciousCandidateRequest(
    val projectId: Int,
    @field:NotBlank(message = "Group ID cannot be blank")
    val groupId: String,
    @field:NotBlank(message = "Artifact ID cannot be blank")
    val artifactId: String,
    @Schema(description = "Stored as the candidate notes and the ban reason")
    @field:NotBlank(message = "Note cannot be blank")
    @field:Size(max = 255, message = "Note must be at most 255 characters")
    val note: String,
)
