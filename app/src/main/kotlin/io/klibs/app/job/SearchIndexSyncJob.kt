package io.klibs.app.job

import io.klibs.app.search.SearchIndexSync
import io.micrometer.core.annotation.Timed
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

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @Timed(
        value = "klibs.search.opensearch.sync.time",
        description = "Klibs: Time taken to rebuild the OpenSearch indices and swap their aliases",
    )
    fun sync() = searchIndexSync.syncAll()
}
