package io.klibs.core.search.opensearch.metrics

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Round-trip time, failures and slow-query logging for the OpenSearch query path.
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchQueryMetrics(
    private val registry: MeterRegistry,
    properties: OpenSearchProperties,
) {

    private val slowQueryThreshold: Duration = properties.slowQueryThreshold

    fun <T> measure(spec: OpenSearchIndexSpec, query: String?, block: () -> T): T {
        val startedAt = System.nanoTime()
        try {
            return block()
        } catch (e: Exception) {
            Counter.builder(QUERY_ERRORS)
                .description("Klibs: OpenSearch queries that failed")
                .tags(tagsOf(spec))
                .register(registry)
                .increment()
            throw e
        } finally {
            val took = Duration.ofNanos(System.nanoTime() - startedAt)
            Timer.builder(QUERY_TIME)
                .description("Klibs: OpenSearch query round trip")
                .tags(tagsOf(spec))
                .register(registry)
                .record(took)
            if (took > slowQueryThreshold) {
                log.warn(
                    "slow OpenSearch query on '{}' took {}ms (threshold {}ms), query: '{}'",
                    spec.base, took.toMillis(), slowQueryThreshold.toMillis(),
                    query.orEmpty().take(MAX_LOGGED_QUERY),
                )
            }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SearchQueryMetrics::class.java)

        private fun tagsOf(spec: OpenSearchIndexSpec) = Tags.of("index", spec.base, "hash", spec.hash)

        private const val QUERY_TIME = "klibs.search.query.time"
        private const val QUERY_ERRORS = "klibs.search.query.errors"
        private const val MAX_LOGGED_QUERY = 200
    }
}
