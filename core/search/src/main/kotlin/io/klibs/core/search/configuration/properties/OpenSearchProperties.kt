package io.klibs.core.search.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * OpenSearch connection + index config.
 */
@ConfigurationProperties("klibs.search.opensearch")
data class OpenSearchProperties(
    val enabled: Boolean = false,
    val uri: String = "http://localhost:9200",
    val projectIndex: String = "project",
    val packageIndex: String = "package",
    val username: String? = null,
    val password: String? = null,

    /** How long to wait for a connection from the HTTP client pool. Apache HC5 defaults to 3 minutes. */
    val requestTimeout: Duration = Duration.ofSeconds(30),

    /** Minimum age before a superseded generation of our own config hash is deleted; the rollback window. */
    val reapMinAge: Duration = Duration.ofHours(1),
    /** Same for generations of other config hashes, which a rolling deploy may still be serving. */
    val foreignReapMinAge: Duration = Duration.ofHours(24),
)
