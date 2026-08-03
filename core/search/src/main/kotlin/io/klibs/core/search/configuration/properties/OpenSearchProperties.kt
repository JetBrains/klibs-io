package io.klibs.core.search.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OpenSearch connection + index config (KTL-4711). v1 = lexical only; semantic/hybrid knobs
 * are Phase 2. `enabled` gates the whole OS stack off for normal app boot / non-OS tests.
 */
@ConfigurationProperties("klibs.search.opensearch")
data class OpenSearchProperties(
    val enabled: Boolean = false,
    val uri: String = "http://localhost:9200",
    val projectIndex: String = "project",
    val packageIndex: String = "package",
    val username: String? = null,
    val password: String? = null,
)
