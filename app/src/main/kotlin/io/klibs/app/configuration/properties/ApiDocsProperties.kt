package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "klibs.api-docs")
data class ApiDocsProperties(
    val title: String,
    val description: String,
    val serverUrls: List<String> = emptyList(),
)
