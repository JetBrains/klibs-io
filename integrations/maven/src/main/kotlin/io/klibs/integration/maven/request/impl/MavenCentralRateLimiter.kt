package io.klibs.integration.maven.request.impl

import io.github.bucket4j.Bucket
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.exception.MavenRateLimitedException
import io.klibs.integration.maven.request.RequestRateLimiter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import kotlin.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Component
class MavenCentralRateLimiter(
    private val mavenIntegrationProperties: MavenIntegrationProperties,
    meterRegistry: MeterRegistry,
    private val clock: Clock = Clock.System,
) : RequestRateLimiter {
    val lastSuccessfulRequestTime = AtomicReference(clock.now())
    private val cooldownUntil = AtomicReference<Instant?>(null)

    init {
        Gauge.builder("klibs.maven.lastSuccessfulRequestTime") {

            (clock.now().toEpochMilliseconds() - lastSuccessfulRequestTime.get()
                .toEpochMilliseconds()).toDouble()
        }
            .description("Time since the last successful Maven API request (ms)")
            .register(meterRegistry)
    }

    private val rateLimitBucket = Bucket.builder()
        .addLimit { limit ->
            limit.capacity(mavenIntegrationProperties.central.rateLimitCapacity)
                .refillGreedy(
                    mavenIntegrationProperties.central.rateLimitRefillAmount,
                    mavenIntegrationProperties.central.rateLimitRefillPeriodSec.seconds.toJavaDuration()
                )
        }.build()

    override fun <T> withRateLimitBlocking(action: () -> T): T {
        checkIfWeareInTooManyRequestsCooldown()

        rateLimitBucket.asBlocking().consume(1)

        val res = action()

        lastSuccessfulRequestTime.set(clock.now())

        return res
    }

    override fun applyCooldown(newRetryAfterTime: Instant) {
        cooldownUntil.updateAndGet { current ->
            if (current == null || newRetryAfterTime > current) newRetryAfterTime else current
        }
    }

    private fun checkIfWeareInTooManyRequestsCooldown() {
        val until = cooldownUntil.get()
        if (until != null && until > clock.now()) {
            throw MavenRateLimitedException.cooldown(until)
        }
    }

    fun available(): Long {
        return rateLimitBucket.availableTokens
    }
}