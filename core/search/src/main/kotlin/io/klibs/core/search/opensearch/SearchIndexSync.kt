package io.klibs.core.search.opensearch

import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchIndexSync(
    private val searchIndexLock: SearchIndexLock,
    private val indexer: OpenSearchIndexer,
    private val indexSpecs: List<OpenSearchIndexSpec>,
) {

    fun syncAll() = run(indexSpecs)

    fun buildMissingAliases() = run(indexSpecs.filterNot { indexer.aliasExists(it) })

    private fun run(targets: List<OpenSearchIndexSpec>) {
        val failures = targets.mapNotNull { runCatching { withLock(it) }.exceptionOrNull() }
        failures.forEach { log.error("OpenSearch index sync failed", it) }
        failures.firstOrNull()?.let { throw it }
    }

    private fun withLock(spec: OpenSearchIndexSpec) {
        val lock = LockSpec("searchIndexSync-${spec.base}-${spec.hash}", LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR)
        val startedAt = Instant.now()
        if (!searchIndexLock.runLocked(lock) { indexer.sync(spec) }) {
            log.info("another pod holds '{}', skipping this run", lock.name)
            return
        }

        val elapsed = Duration.between(startedAt, Instant.now())
        if (elapsed > LOCK_AT_MOST_FOR) {
            log.warn("'{}' took {}, longer than lockAtMostFor {}", lock.name, elapsed, LOCK_AT_MOST_FOR)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SearchIndexSync::class.java)

        private val LOCK_AT_MOST_FOR = Duration.ofMinutes(5)
        private val LOCK_AT_LEAST_FOR = Duration.ofMinutes(5)
    }
}
