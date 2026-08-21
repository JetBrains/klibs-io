package io.klibs.core.search.opensearch.metrics

import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.opensearch.OpenSearchIndexer
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Freshness and outcome of the OpenSearch index rebuild.
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchIndexMetrics(
    private val registry: MeterRegistry,
    private val indexer: OpenSearchIndexer,
    indexSpecs: List<OpenSearchIndexSpec>,
) {

    init {
        indexSpecs.forEach { spec ->
            Gauge.builder(INDEX_AGE) { ageSeconds(spec) }
                .description("Klibs: Seconds since the index behind this alias was created")
                .tags(tagsOf(spec))
                .register(registry)
            Gauge.builder(INDEX_DOCS) { read(spec) { servingDocCount(spec).toDouble() } }
                .description("Klibs: Documents the index behind this alias serves")
                .tags(tagsOf(spec))
                .register(registry)
        }
    }

    fun recordSuccess(spec: OpenSearchIndexSpec, took: Duration) {
        Timer.builder(SYNC_DURATION)
            .description("Klibs: Time to rebuild one index and swap its alias")
            .tags(tagsOf(spec))
            .register(registry)
            .record(took)
    }

    fun recordFailure(spec: OpenSearchIndexSpec) {
        Counter.builder(SYNC_FAILURES)
            .description("Klibs: Number of failed OpenSearch index rebuilds")
            .tags(tagsOf(spec))
            .register(registry)
            .increment()
    }

    private fun ageSeconds(spec: OpenSearchIndexSpec): Double = read(spec) {
        servingIndexCreatedAt(spec)?.let { Duration.between(it, Instant.now()).seconds.toDouble() }
    }

    private fun read(spec: OpenSearchIndexSpec, value: OpenSearchIndexer.() -> Double?): Double =
        runCatching { indexer.value() }
            .onFailure { log.debug("could not read the state of '{}'", spec.alias, it) }
            .getOrNull()
            ?: Double.NaN

    private companion object {
        private val log = LoggerFactory.getLogger(SearchIndexMetrics::class.java)

        fun tagsOf(spec: OpenSearchIndexSpec) = Tags.of("index", spec.base, "hash", spec.hash)

        const val SYNC_FAILURES = "klibs.search.index.sync.failures"
        const val INDEX_AGE = "klibs.search.index.age.seconds"
        const val INDEX_DOCS = "klibs.search.index.docs"
        const val SYNC_DURATION = "klibs.search.index.sync.duration"
    }
}
