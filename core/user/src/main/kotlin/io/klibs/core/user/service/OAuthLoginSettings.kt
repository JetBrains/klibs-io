package io.klibs.core.user.service

import java.time.Duration

data class OAuthLoginSettings(
    val attemptTtl: Duration,
    val trustedFrontendOrigin: String,
)
