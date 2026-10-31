package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "Applies human review decisions to suspicious package candidates")
data class ReviewSuspiciousCandidatesRequest(
    @field:NotEmpty(message = "At least one decision is required")
    @field:Valid
    val decisions: List<ReviewSuspiciousCandidateDecision>,
)

data class ReviewSuspiciousCandidateDecision(
    val projectId: Int,
    @field:NotBlank(message = "Group ID cannot be blank")
    val groupId: String,
    val decision: SuspiciousCandidateDecision,
    @field:NotEmpty(message = "At least one artifact is required")
    val artifacts: List<@NotBlank String>,
    @field:NotBlank(message = "Note cannot be blank")
    @field:Size(max = 255, message = "Note must be at most 255 characters")
    val note: String,
)

enum class SuspiciousCandidateDecision {
    KEEP,
    BAN,
}