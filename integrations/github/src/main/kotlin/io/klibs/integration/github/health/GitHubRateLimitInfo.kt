package io.klibs.integration.github.health

import java.time.Instant

data class GitHubRateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetAt: Instant,
)
