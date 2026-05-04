package io.klibs.integration.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.GitHub
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Per-HTTP-request counting (`klibs.github.requests`) lives on an OkHttp interceptor and is
 * covered by [GitHubRequestMeteringInterceptorTest]. This class covers the remaining
 * library-level metrics — the `klibs.github.request.time` timer and the
 * `klibs.github.lastSuccessfulRequestTime` gauge.
 */
@ExtendWith(MockitoExtension::class)
class GitHubMetricsTest {

    private lateinit var meterRegistry: SimpleMeterRegistry

    @Mock
    private lateinit var githubApi: GitHub

    private lateinit var gitHubIntegration: GitHubIntegration
    private val klibsRepoName = "JetBrains/klibs-io"
    private val processedLabel = "triaged"

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()

        gitHubIntegration = GitHubIntegrationKohsukeLibrary(
            meterRegistry,
            githubApi,
            OkHttpClient(),
            GitHubIntegrationProperties(
                cache = GitHubIntegrationProperties.Cache(),
            ),
            jacksonObjectMapper(),
            klibsRepoName,
            processedLabel,
        )
    }

    @Test
    fun `records request time timer for kohsuke-backed call`() {
        gitHubIntegration.getRepository("kotlin", "dokka")

        val timer = meterRegistry.timer("klibs.github.request.time")
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0)
    }

    @Test
    fun `records last successful request time gauge`() {
        val gauge = meterRegistry.find("klibs.github.lastSuccessfulRequestTime").gauge()
        assertNotNull(gauge)

        // Ensure some time passes so the initial value is > 0
        Thread.sleep(10)
        val initialValue = gauge.value()
        assertTrue(initialValue >= 10, "Initial gauge value should be at least 10ms. Actual: $initialValue")

        gitHubIntegration.getRepository("kotlin", "dokka")

        val updatedValue = gauge.value()
        assertTrue(
            updatedValue < initialValue,
            "Gauge value should have decreased (reset to ~0). Initial: $initialValue, Updated: $updatedValue"
        )
        assertTrue(updatedValue < 5, "Updated gauge value should be near zero. Actual: $updatedValue")
    }
}
