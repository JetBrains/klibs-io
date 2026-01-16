package io.klibs.integration.github

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.GitHub
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that verify GitHub metrics are collected and reported correctly.
 */
@ExtendWith()
class GitHubMetricsTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    
    private lateinit var githubApi: GitHub
    
    private lateinit var gitHubIntegration: GitHubIntegration

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        
        githubApi = mockk()
        gitHubIntegration = GitHubIntegrationKohsukeLibrary(
            meterRegistry,
            githubApi,
            OkHttpClient(),
            GitHubIntegrationProperties(
                cache = GitHubIntegrationProperties.Cache(),
            )
        )
    }

    @Test
    fun `should record repository request metrics`() {
        val initialCount = meterRegistry.counter("klibs.github.requests", "type", "repository").count()
        assertEquals(0.0, initialCount)

        gitHubIntegration.getRepository("kotlin", "dokka")

        val updatedCount = meterRegistry.counter("klibs.github.requests", "type", "repository").count()
        assertEquals(1.0, updatedCount)

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `should record user request metrics`() {
        val initialCount = meterRegistry.counter("klibs.github.requests", "type", "user").count()
        assertEquals(0.0, initialCount)

        gitHubIntegration.getUser("bnorm")

        val updatedCount = meterRegistry.counter("klibs.github.requests", "type", "user").count()
        assertEquals(1.0, updatedCount)

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `should record license request metrics`() {
        val initialCount = meterRegistry.counter("klibs.github.requests", "type", "license").count()
        assertEquals(0.0, initialCount)

        gitHubIntegration.getLicense(DOKKA_REPOSITORY_ID)

        val updatedCount = meterRegistry.counter("klibs.github.requests", "type", "license").count()
        assertEquals(1.0, updatedCount)

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `should record readme request metrics`() {
        val initialCount = meterRegistry.counter("klibs.github.requests", "type", "readme").count()
        assertEquals(0.0, initialCount)

        gitHubIntegration.getReadmeWithModifiedSinceCheck(DOKKA_REPOSITORY_ID)

        val updatedCount = meterRegistry.counter("klibs.github.requests", "type", "readme").count()
        assertEquals(1.0, updatedCount)

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `should record markdown request metrics`() {
        val initialCount = meterRegistry.counter("klibs.github.requests", "type", "markdown").count()
        assertEquals(0.0, initialCount)

        gitHubIntegration.markdownToHtml("# Test", DOKKA_REPOSITORY_ID)

        val updatedCount = meterRegistry.counter("klibs.github.requests", "type", "markdown").count()
        assertEquals(1.0, updatedCount)

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `should record all metrics for multiple requests`() {
        gitHubIntegration.getRepository("kotlin", "dokka")
        gitHubIntegration.getUser("bnorm")
        gitHubIntegration.getLicense(DOKKA_REPOSITORY_ID)
        gitHubIntegration.getReadmeWithModifiedSinceCheck(DOKKA_REPOSITORY_ID)
        gitHubIntegration.markdownToHtml("# Test", DOKKA_REPOSITORY_ID)

        assertEquals(1.0, meterRegistry.counter("klibs.github.requests", "type", "repository").count())
        assertEquals(1.0, meterRegistry.counter("klibs.github.requests", "type", "user").count())
        assertEquals(1.0, meterRegistry.counter("klibs.github.requests", "type", "license").count())
        assertEquals(1.0, meterRegistry.counter("klibs.github.requests", "type", "readme").count())
        assertEquals(1.0, meterRegistry.counter("klibs.github.requests", "type", "markdown").count())

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(5, timer.count())
    }

    @Test
    fun `should record last successful request time gauge`() {
        val gauge = meterRegistry.find("klibs.github.lastSuccessfulRequestTime").gauge()
        assertNotNull(gauge)

        // Ensure some time passes so the initial value is > 0
        Thread.sleep(10)
        val initialValue = gauge.value()
        assertTrue(initialValue >= 10, "Initial gauge value should be at least 10ms. Actual: $initialValue")

        gitHubIntegration.getRepository("kotlin", "dokka")

        val updatedValue = gauge.value()
        assertTrue(updatedValue < initialValue, "Gauge value should have decreased (reset to ~0). Initial: $initialValue, Updated: $updatedValue")
        assertTrue(updatedValue < 5, "Updated gauge value should be near zero. Actual: $updatedValue")
    }

    companion object {
        private const val DOKKA_REPOSITORY_ID = 21763603L // https://github.com/kotlin/dokka
    }
}