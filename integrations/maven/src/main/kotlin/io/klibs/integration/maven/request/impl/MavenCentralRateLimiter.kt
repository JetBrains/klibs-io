package io.klibs.integration.maven.request.impl

import io.github.bucket4j.Bucket
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.request.RequestRateLimiter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class MavenCentralRateLimiter(
    private val mavenIntegrationProperties: MavenIntegrationProperties,
    meterRegistry: MeterRegistry
) : RequestRateLimiter {
    val lastSuccessfulRequestTime = AtomicReference(Instant.now())

    init {
        Gauge.builder("klibs.maven.lastSuccessfulRequestTime") {
            (Instant.now().toEpochMilli() - lastSuccessfulRequestTime.get().toEpochMilli()).toDouble()
        }
            .description("Time since the last successful Maven API request (ms)")
            .register(meterRegistry)
    }

    private val rateLimitBucket = Bucket.builder()
        .addLimit { limit ->
            limit.capacity(mavenIntegrationProperties.central.rateLimitCapacity)
                .refillGreedy(
                    mavenIntegrationProperties.central.rateLimitRefillAmount,
                    Duration.ofSeconds(mavenIntegrationProperties.central.rateLimitRefillPeriodSec)
                )
        }.build()

    override fun <T> withRateLimitBlocking(action: () -> T): T {
        rateLimitBucket.asBlocking().consume(1)

        val res = action()

        lastSuccessfulRequestTime.set(Instant.now())

        return res
    }

    fun available(): Long {
        return rateLimitBucket.availableTokens
    }
}