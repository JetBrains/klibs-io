package io.klibs.app.search

import io.klibs.core.search.opensearch.IndexNaming
import io.klibs.core.search.opensearch.IndexSpec
import io.klibs.core.search.opensearch.OpenSearchIndexer
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchIndexSync(
    private val lockingTaskExecutor: LockingTaskExecutor,
    private val indexer: OpenSearchIndexer,
    private val naming: IndexNaming,
) {

    fun syncAll() = run(naming.all)

    fun buildMissingAliases() = run(naming.all.filterNot { indexer.aliasExists(it) })

    private fun run(targets: List<IndexSpec>) {
        val failures = targets.mapNotNull { runCatching { withLock(it) }.exceptionOrNull() }
        failures.forEach { log.error("OpenSearch index sync failed", it) }
        failures.firstOrNull()?.let { throw it }
    }

    private fun withLock(spec: IndexSpec) {
        val lock = LockConfiguration(
            Instant.now(),
            "searchIndexSync-${spec.base}-${spec.hash}",
            LOCK_AT_MOST_FOR,
            LOCK_AT_LEAST_FOR,
        )
        val startedAt = Instant.now()
        val task = LockingTaskExecutor.TaskWithResult<Unit> { indexer.sync(spec) }
        if (!lockingTaskExecutor.executeWithLock(task, lock).wasExecuted()) {
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
