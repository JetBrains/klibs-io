package io.klibs.app.api

import java.time.Instant

data class SuspiciousCandidateResponse(
    val projectId: Int,
    val groupId: String,
    val artifactId: String,
    val detectedAt: Instant,
)
