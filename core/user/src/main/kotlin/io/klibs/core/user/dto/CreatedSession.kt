package io.klibs.core.user.dto

import java.time.Instant

data class CreatedSession(
    val token: String,
    val expiresAt: Instant,
)
