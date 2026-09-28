package io.klibs.core.user.model

import io.klibs.core.user.entity.UserEntity
import java.time.Duration

data class AuthenticatedSession(
    val user: UserEntity,
    val expirationRefreshed: Boolean,
    val remainingTtl: Duration,
)
