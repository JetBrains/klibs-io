package io.klibs.integration.maven.request.impl

import io.klibs.integration.maven.request.RequestRateLimiter
import org.springframework.stereotype.Component

@Component
class UnlimitedRateLimiter : RequestRateLimiter {
    override fun <T> withRateLimitBlocking(action: () -> T): T {
        return action.invoke()
    }
}