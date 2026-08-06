package io.klibs.app.search

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    value = ["klibs.search.opensearch.enabled", "klibs.scheduling.search-index-sync.enabled"],
    havingValue = "true",
)
class SearchIndexBootstrap(
    private val searchIndexSync: SearchIndexSync,
    private val searchIndexReadiness: SearchIndexReadiness,
) : SmartLifecycle {

    private var running = false

    override fun start() {
        running = true

        runCatching { searchIndexSync.buildMissingAliases() }
            .onFailure { log.error("boot build of the OpenSearch indices failed", it) }
        searchIndexReadiness.refresh()
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running

    /** Below the web server's own phase (`DEFAULT_PHASE - 2048`), so it starts first. */
    override fun getPhase(): Int = SmartLifecycle.DEFAULT_PHASE - 4096

    private companion object {
        private val log = LoggerFactory.getLogger(SearchIndexBootstrap::class.java)
    }
}
