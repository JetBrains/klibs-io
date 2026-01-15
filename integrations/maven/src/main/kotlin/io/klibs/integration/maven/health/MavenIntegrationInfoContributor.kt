package io.klibs.integration.maven.health

import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.MavenIntegrationProperties
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MavenIntegrationInfoContributor(
    private val mavenIntegrationProperties: MavenIntegrationProperties,
    private val mavenCentralRateLimiter: MavenCentralRateLimiter,
) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        builder.withDetail(
            "mavenCentral", mapOf(
                "rateLimit" to mapOf(
                    "capacity" to mavenIntegrationProperties.central.rateLimitCapacity,
                    "available" to mavenCentralRateLimiter.available(),
                    "refillAmount" to mavenIntegrationProperties.central.rateLimitRefillAmount,
                    "refillPeriodSec" to mavenIntegrationProperties.central.rateLimitRefillPeriodSec,
                ),
                "now" to Instant.now(),
                "lastSuccessfulRequest" to mavenCentralRateLimiter.lastSuccessfulRequestTime.get()
            )
        )
    }
}
