package io.klibs.integration.maven

import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MavenMetricsTest {

    @Test
    fun `should record last successful request time gauge`() {
        val meterRegistry = SimpleMeterRegistry()
        val properties = MavenIntegrationProperties(
            central = MavenIntegrationProperties.Central(
                rateLimitCapacity = 100,
                rateLimitRefillAmount = 10,
                rateLimitRefillPeriodSec = 60,
                discoveryEndpoint = "http://localhost/discovery",
                indexEndpoint = "http://localhost/index",
                indexDir = "/tmp/maven-index"
            )
        )
        
        val limiter = MavenCentralRateLimiter(properties, meterRegistry)

        val gauge = meterRegistry.find("klibs.maven.lastSuccessfulRequestTime").gauge()
        assertNotNull(gauge)

        // Ensure some time passes so the initial value is > 0
        Thread.sleep(10)
        val initialValue = gauge.value()
        assertTrue(initialValue >= 10, "Initial gauge value should be at least 10ms. Actual: $initialValue")

        limiter.withRateLimitBlocking {
            // successful request
        }

        val updatedValue = gauge.value()
        assertTrue(updatedValue < initialValue, "Gauge value should have decreased (reset to ~0). Initial: $initialValue, Updated: $updatedValue")
        assertTrue(updatedValue < 5, "Updated gauge value should be near zero. Actual: $updatedValue")
    }
}
