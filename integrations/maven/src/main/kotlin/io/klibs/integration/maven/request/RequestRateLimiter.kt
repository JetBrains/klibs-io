package io.klibs.integration.maven.request

interface RequestRateLimiter {
    fun <T> withRateLimitBlocking(action: () -> T): T
}