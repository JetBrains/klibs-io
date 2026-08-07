package io.klibs.core.search.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * OpenSearch connection + index config.
 */
@ConfigurationProperties("klibs.search.opensearch")
data class OpenSearchProperties(
    val enabled: Boolean = false,
    val uri: String = "https://localhost:9200",
    val projectIndex: String = "project",
    val packageIndex: String = "package",
    val username: String? = null,
    val password: String? = null,

    /** Over https, skip cert/hostname verification (trust any cert). Keep false in prod. */
    val trustAllCertificates: Boolean = false,

    /** How long to wait for a connection from the HTTP client pool. Apache HC5 defaults to 3 minutes. */
    val requestTimeout: Duration = Duration.ofSeconds(30),
)
