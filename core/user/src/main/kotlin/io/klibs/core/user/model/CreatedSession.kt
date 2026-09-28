package io.klibs.core.user.model

import java.time.Instant

class CreatedSession(
    val token: String,
    val expiresAt: Instant,
)
