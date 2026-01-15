package io.klibs.integration.github.health

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.GitHubIntegrationProperties
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GitHubIntegrationInfoContributor(
    private val gitHubIntegration: GitHubIntegration,
    private val gitHubIntegrationProperties: GitHubIntegrationProperties
) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        val rateLimitInfo = gitHubIntegration.getRateLimitInfo()
        builder.withDetail(
            "gitHub", mapOf(
                "rateLimit" to mapOf(
                    "limit" to rateLimitInfo.limit,
                    "remaining" to rateLimitInfo.remaining,
                    "resetDate" to rateLimitInfo.resetAt,
                ),
                "cache" to mapOf(
                    "requestCachePath" to gitHubIntegrationProperties.cache.requestCachePath,
                    "requestCacheSizeMb" to gitHubIntegrationProperties.cache.requestCacheSizeMb,
                ),
                "now" to Instant.now(),
                "lastSuccessfulRequest" to gitHubIntegration.getLastSuccessfulRequestTime()
            )
        )
    }
}

