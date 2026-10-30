package io.klibs.app.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(
    name = "SuspiciousCandidate",
    description = "Data about a suspicious candidate (artifactId clash in a single project)"
)
data class SuspiciousCandidateResponse(
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
        description = "Artifact ID of the Maven artifact",
        example = "kstatemachine"
    )
    val artifactId: String,
    @Schema(
        description = "Instant when the suspicious candidate was detected",
        example = "2023-03-01T12:00:00Z"
    )
    val detectedAt: Instant,
)
