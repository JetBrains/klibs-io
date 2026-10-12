package io.klibs.core.user.dto

import java.time.Duration
import java.util.UUID

/** Session validation result returned to the HTTP layer. */
data class AuthenticatedSessionDTO(
    val userId: UUID,
    val refreshedCookieTtl: Duration?,
)
