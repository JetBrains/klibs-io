package io.klibs.integration.maven.request

import kotlin.time.Instant

interface RequestRateLimiter {
    fun <T> withRateLimitBlocking(action: () -> T): T

    /**
     * This method is designed to set a cooldown on new requests
     * in case response code 429 (Too Many Requests) was received.
     */
    fun applyCooldown(newRetryAfterTime: Instant) {
        // no-op by default
    }
}