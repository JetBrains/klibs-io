package io.klibs.app.search

import io.klibs.core.search.opensearch.IndexNaming
import io.klibs.core.search.opensearch.OpenSearchIndexer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchIndexReadiness(
    private val indexer: OpenSearchIndexer,
    private val naming: IndexNaming,
) {

    private val ready = AtomicBoolean(false)

    fun isReady(): Boolean = ready.get() || refresh()

    fun refresh(): Boolean {
        val aliasesReady = runCatching {
            indexer.aliasExists(naming.project) && indexer.aliasExists(naming.packages)
        }.getOrDefault(false)
        if (aliasesReady) ready.set(true)
        return ready.get()
    }
}
