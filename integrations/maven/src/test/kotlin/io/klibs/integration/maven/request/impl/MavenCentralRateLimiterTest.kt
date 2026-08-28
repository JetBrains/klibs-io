package io.klibs.integration.maven.request.impl

import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.exception.MavenRateLimitedException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes

class MavenCentralRateLimiterTest {

    private val mockedNow = Instant.parse("2026-08-31T12:00:00Z")
    private val mockedClock = object : Clock {
        override fun now(): Instant = mockedNow
    }

    private fun newLimiter(clock: Clock = Clock.System): MavenCentralRateLimiter {
        val properties = MavenIntegrationProperties(
            central = MavenIntegrationProperties.Central(
                rateLimitCapacity = 1000,
                rateLimitRefillAmount = 1000,
                rateLimitRefillPeriodSec = 1,
                indexEndpoint = "https://index",
                indexDir = "/tmp",
                contentEndpoint = "https://content",
                contentFallbackEndpoint = "https://fallback",
            )
        )
        return MavenCentralRateLimiter(properties, SimpleMeterRegistry(), clock)
    }

    @Test
    fun `cooldown makes withRateLimitBlocking throw MavenRateLimitedException without running action`() {
        val uut = newLimiter(mockedClock)
        uut.applyCooldown(mockedNow.plus(30.minutes))

        var invoked = false
        val ex = assertFailsWith<MavenRateLimitedException> {
            uut.withRateLimitBlocking { invoked = true }
        }

        assertEquals(false, invoked, "Action must not run while cooldown is active")
        assertEquals(mockedNow.plus(30.minutes), ex.retryAfter)
    }

    @Test
    fun `request proceeds after the cooldown window elapses`() {
        val uut = newLimiter(mockedClock)
        uut.applyCooldown(mockedNow.minus(30.minutes))

        val result = uut.withRateLimitBlocking { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun `overlapping cooldown extends rather than shortens the window`() {
        val uut = newLimiter(mockedClock)
        uut.applyCooldown(mockedNow.minus(30.minutes))
        uut.applyCooldown(mockedNow.plus(30.minutes))

        val ex = assertFailsWith<MavenRateLimitedException> {
            uut.withRateLimitBlocking { }
        }

        assertEquals(mockedNow.plus(30.minutes), ex.retryAfter)
    }
}
