package io.klibs.integration.github.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File

@ConfigurationProperties("klibs.integration.github")
data class GitHubIntegrationProperties(
    val personalAccessToken: String? = null,
    val cache: Cache,
    val webhook: Webhook,
    val indexRequests: IndexRequests
) {
    data class Cache(
        val requestCachePath: File? = null,
        val requestCacheSizeMb: Int? = null,
    )

    data class Webhook(
        val secret: String? = null
    )

    data class IndexRequests(
        val repository: String? = null,
        val requestLabel: String? = null,
        val processedLabel: String = "",
        val developerHandle: String? = null
    )
}