package io.klibs.integration.maven

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "klibs.integration.maven")
data class MavenIntegrationProperties(
    val central: Central,
) {
    data class Central(
        val rateLimitCapacity: Long,
        val rateLimitRefillAmount: Long,
        val rateLimitRefillPeriodSec: Long,
        val discoveryEndpoint: String,
        val indexEndpoint: String = "https://repo1.maven.org/maven2",
        val indexDir: String = "cache/maven-index",
    )
}
