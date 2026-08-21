package io.klibs.core.search.opensearch.metrics

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SearchQueryMetricsTest {

    private val registry = SimpleMeterRegistry()
    private val metrics = SearchQueryMetrics(registry, OpenSearchProperties(slowQueryThreshold = Duration.ofMillis(1)))

    private val project = spec("project")
    private val packages = spec("package")

    @Test
    fun `a successful query is timed and counts no error`() {
        assertEquals("hits", metrics.measure(project, "ktor") { "hits" })

        assertEquals(1L, timer("project").count())
        assertNull(registry.find("klibs.search.query.errors").tag("index", "project").counter())
    }

    @Test
    fun `the index definition hash is reported as its own tag`() {
        metrics.measure(project, "ktor") { }

        assertEquals(project.hash, timer("project").id.getTag("hash"))
    }

    @Test
    fun `a failing query is counted and the failure still propagates`() {
        assertFailsWith<IllegalStateException> {
            metrics.measure(packages, "ktor") { throw IllegalStateException("cluster down") }
        }

        assertEquals(1.0, registry.get("klibs.search.query.errors").tag("index", "package").counter().count())
        assertEquals(1L, timer("package").count())
    }

    @Test
    fun `indices are timed separately`() {
        metrics.measure(project, null) { }
        metrics.measure(packages, null) { }
        metrics.measure(packages, null) { }

        assertEquals(1L, timer("project").count())
        assertEquals(2L, timer("package").count())
    }

    private fun timer(index: String) = registry.get("klibs.search.query.time").tag("index", index).timer()

    private fun spec(base: String) = OpenSearchIndexSpec(
        base = base,
        settings = "{}",
        mappings = "{}",
        sql = "select 1",
    ) { it.toString() }
}
