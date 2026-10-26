package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "SuspiciousCandidateReviewResult",
    description = "Outcome of applying one review decision. KEEP yields one row per groupId, BAN yields one row per artifact."
)
data class SuspiciousCandidateReviewResult(
    @Schema(
        description = "Unique id of the parent project.",
        example = "4"
    )
    val projectId: Int,
    @Schema(
        description = "Group ID of the Maven artifact",
        example = "io.github.nsk90"
    )
    val groupId: String,
    @Schema(
        description = "Artifact ID of the Maven artifact. Null for KEEP, which is applied to the whole groupId.",
        example = "kstatemachine",
        nullable = true
    )
    val artifactId: String?,
    @Schema(
        description = "Decision that was applied",
        example = "BAN"
    )
    val decision: SuspiciousCandidateDecision,
    @Schema(
        description = "OK: applied. SKIP: nothing changed (no PENDING rows, already banned, " +
            "or new PENDING artifacts since the review); do not retry. FAIL: unexpected error, see message.",
        example = "OK"
    )
    val outcome: SuspiciousCandidateReviewOutcome,
    @Schema(
        description = "Human-readable details about the outcome",
        example = "Resolved and banned"
    )
    val message: String,
)

@Schema(description = "Result of applying a single review decision")
enum class SuspiciousCandidateReviewOutcome {
    OK,
    SKIP,
    FAIL,
}
