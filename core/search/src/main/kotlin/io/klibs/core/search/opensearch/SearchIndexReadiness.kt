package io.klibs.core.search.opensearch

import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class SearchIndexReadiness(
    private val indexer: OpenSearchIndexer,
    private val indexSpecs: List<OpenSearchIndexSpec>,
) {

    private val ready = AtomicBoolean(false)

    fun isReady(): Boolean = ready.get() || refresh()

    fun refresh(): Boolean {
        val aliasesReady = runCatching {
            indexSpecs.all { indexer.aliasExists(it) }
        }.getOrDefault(false)
        if (aliasesReady) ready.set(true)
        return ready.get()
    }
}
