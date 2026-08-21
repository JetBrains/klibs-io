package io.klibs.core.search.opensearch.metrics

import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.opensearch.OpenSearchIndexer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchIndexMetricsTest {

    private val registry = SimpleMeterRegistry()
    private val indexer = mock<OpenSearchIndexer>()

    private val project = spec("project")
    private val packages = spec("package")

    private val metrics = SearchIndexMetrics(registry, indexer, listOf(project, packages))

    @Test
    fun `freshness is the age of the index the alias serves, not of this pod's last rebuild`() {
        whenever(indexer.servingIndexCreatedAt(project)).thenReturn(Instant.now().minusSeconds(120))

        // No rebuild happened on this pod: the pod that lost the ShedLock race still reports the age.
        assertEquals(120.0, gauge("klibs.search.index.age.seconds", "project"))
    }

    @Test
    fun `a failed read reports no freshness, and the next scrape reads the cluster again`() {
        whenever(indexer.servingIndexCreatedAt(project))
            .thenThrow(RuntimeException("cluster is down"))
            .thenReturn(Instant.now().minusSeconds(30))

        assertTrue(gauge("klibs.search.index.age.seconds", "project").isNaN())
        assertEquals(30.0, gauge("klibs.search.index.age.seconds", "project"))
    }

    @Test
    fun `an alias serving no index reports no freshness`() {
        whenever(indexer.servingIndexCreatedAt(project)).thenReturn(null)

        assertTrue(gauge("klibs.search.index.age.seconds", "project").isNaN())
    }

    @Test
    fun `the document count is the one the alias serves, not of this pod's last rebuild`() {
        whenever(indexer.servingDocCount(project)).thenReturn(1_234)

        // No rebuild happened on this pod: the pod that lost the ShedLock race still reports the count.
        assertEquals(1_234.0, gauge("klibs.search.index.docs", "project"))
        assertEquals(project.hash, registry.get("klibs.search.index.docs").tag("index", "project").gauge()
            .id.getTag("hash"))
    }

    @Test
    fun `an unreadable document count reports no value rather than zero`() {
        whenever(indexer.servingDocCount(project)).thenThrow(RuntimeException("cluster is down"))

        assertTrue(gauge("klibs.search.index.docs", "project").isNaN())
    }

    @Test
    fun `a successful rebuild reports its duration`() {
        metrics.recordSuccess(project, took = Duration.ofSeconds(4))

        assertEquals(4.0, syncSeconds("project"))
        assertEquals(1L, syncCount("project"))
    }

    @Test
    fun `indices are reported separately`() {
        whenever(indexer.servingDocCount(project)).thenReturn(10)
        whenever(indexer.servingDocCount(packages)).thenReturn(20)
        metrics.recordSuccess(project, took = Duration.ofSeconds(1))
        metrics.recordFailure(packages)

        assertEquals(10.0, gauge("klibs.search.index.docs", "project"))
        assertEquals(20.0, gauge("klibs.search.index.docs", "package"))
        assertNull(registry.find("klibs.search.index.sync.failures").tag("index", "project").counter())
        assertEquals(1.0, failures("package"))
    }

    private fun gauge(name: String, index: String): Double =
        registry.get(name).tag("index", index).gauge().value()

    private fun syncSeconds(index: String): Double = registry.get("klibs.search.index.sync.duration")
        .tag("index", index).timer().totalTime(TimeUnit.SECONDS)

    private fun syncCount(index: String): Long =
        registry.get("klibs.search.index.sync.duration").tag("index", index).timer().count()

    private fun failures(index: String): Double =
        registry.get("klibs.search.index.sync.failures").tag("index", index).counter().count()

    private fun spec(base: String) = OpenSearchIndexSpec(
        base = base,
        settings = "{}",
        mappings = "{}",
        sql = "select 1",
    ) { it.toString() }
}
