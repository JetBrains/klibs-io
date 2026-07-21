package io.klibs.app.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Shared retry limit for the indexing/reporting queues. Referenced both by the
 * queue queries (SpEL) and by the report writer, so the cap can't drift apart.
 */
@Configuration
@ConfigurationProperties(prefix = "klibs.indexing-configuration.retry")
class IndexingRetryConfiguration {
    var maxAttempts: Int = 2
}
