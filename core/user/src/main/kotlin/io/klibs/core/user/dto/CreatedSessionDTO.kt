package io.klibs.core.user.dto

import java.time.Instant

data class CreatedSessionDTO(
    val token: String,
    val expiresAt: Instant,
)
