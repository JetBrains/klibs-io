package io.klibs.app.job

import io.klibs.core.search.opensearch.SearchIndexSync
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(
    value = ["klibs.search.opensearch.enabled", "klibs.scheduling.search-index-sync.enabled"],
    havingValue = "true",
)
class SearchIndexSyncJob(
    private val searchIndexSync: SearchIndexSync,
) {

    // Duration is measured per index by SearchIndexMetrics; a timer here would only add
    // the lock wait and would score a skipped run as a fast success.
    @Scheduled(
        scheduler = "searchIndexSyncScheduler",
        initialDelay = 0,
        fixedRate = 10,
        timeUnit = TimeUnit.MINUTES,
    )
    fun sync() = searchIndexSync.syncAll()
}
