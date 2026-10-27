package io.klibs.core.user.service

import java.time.Duration

data class UserSessionSettings(
    val idleTtl: Duration,
    val refreshInterval: Duration,
    val absoluteTtl: Duration,
)
