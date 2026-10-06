package io.klibs.core.user.dto

import io.klibs.core.user.entity.UserEntity
import java.time.Duration

/**
 * The session validation result that tells the HTTP layer whether to reissue the cookie and with what TTL.
 */
data class AuthenticatedSession(
    val user: UserEntity,
    val expirationRefreshed: Boolean,
    val remainingTtl: Duration,
)
