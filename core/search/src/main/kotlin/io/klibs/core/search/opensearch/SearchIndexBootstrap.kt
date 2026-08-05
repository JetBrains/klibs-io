package io.klibs.core.search.opensearch

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    value = ["klibs.search.opensearch.enabled", "klibs.scheduling.search-index-sync.enabled"],
    havingValue = "true",
)
class SearchIndexBootstrap(
    private val searchIndexSync: SearchIndexSync,
    private val searchIndexReadiness: SearchIndexReadiness,
) {

    /** Refresh runs `@PostConstruct` before the web server starts, so no traffic arrives index-less. */
    @PostConstruct
    fun buildMissingIndices() {
        runCatching { searchIndexSync.buildMissingAliases() }
            .onFailure { log.error("boot build of the OpenSearch indices failed", it) }
        searchIndexReadiness.refresh()
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SearchIndexBootstrap::class.java)
    }
}
