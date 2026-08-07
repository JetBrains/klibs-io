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

    val connectTimeout: Duration = Duration.ofSeconds(5),
    val socketTimeout: Duration = Duration.ofSeconds(30),
    val requestTimeout: Duration = Duration.ofSeconds(30),

    val reapMinAge: Duration = Duration.ofHours(1),
    val foreignReapMinAge: Duration = Duration.ofHours(24),
)
