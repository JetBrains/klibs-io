package io.klibs.app.util

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class BackoffProvider(
    private val jobName: String,
    meterRegistry: MeterRegistry,
) {
    private val backoffStates = ConcurrentHashMap<Int, BackoffState>()

    private data class BackoffState(
        val attempts: Int,
        val nextAllowedAt: Instant,
    )

    init {
        Gauge
            .builder(METRIC_NAME) { backoffStates.size.toDouble() }
            .tag("job", jobName)
            .description("Number of ids currently in backoff state for a given job")
            .register(meterRegistry)
    }

    fun isBackedOff(id: Int, now: Instant = Instant.now()): Boolean {
        val state = backoffStates[id] ?: return false
        return now.isBefore(state.nextAllowedAt)
    }

    fun onFailure(id: Int, now: Instant = Instant.now()) {
        val previous = backoffStates[id]
        val attempts = (previous?.attempts ?: 0) + 1
        val delay = computeBackoffDelay(attempts)
        val nextAllowedAt = now.plus(delay)
        backoffStates[id] = BackoffState(attempts, nextAllowedAt)
        logger.warn("Job $jobName: Backoff for id=$id attempts=$attempts nextAllowedAt=$nextAllowedAt delay=${delay.toSeconds()}s")
    }

    fun onSuccess(id: Int) {
        backoffStates.remove(id)
    }

    private fun computeBackoffDelay(attempts: Int): Duration {
        // Base 60s, exponential factor 2, capped at 1 hour
        val base = 60L
        val exp = 1L shl (attempts - 1).coerceAtMost(10) // cap growth
        val seconds = (base * exp).coerceAtMost(3600L)
        return Duration.ofSeconds(seconds)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BackoffProvider::class.java)
        const val METRIC_NAME = "klibs.backoff.states.size"
    }
}