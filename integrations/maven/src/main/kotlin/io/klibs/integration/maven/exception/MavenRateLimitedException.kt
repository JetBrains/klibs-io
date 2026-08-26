package io.klibs.integration.maven.exception

import kotlin.time.Instant

/**
 * Exception thrown when requests to Maven Central are rate-limited.
 *
 * @param message The detailed error message describing the rate limit condition.
 * @param retryAfter The Instant indicating when the client can retry the request, or `null` if unknown.
 */
class MavenRateLimitedException(
    message: String,
    val retryAfter: Instant? = null,
) : RuntimeException(message) {
    companion object {
        fun forUrl(url: String, retryAfter: Instant? = null) = MavenRateLimitedException(
            "Rate limited by Maven Central on $url" + (retryAfter?.let { ", retry after $retryAfter" } ?: ""),
            retryAfter,
        )

        fun cooldown(retryAfter: Instant) = MavenRateLimitedException(
            "Maven Central cooldown active, retry after $retryAfter",
            retryAfter,
        )
    }
}
