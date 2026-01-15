package io.klibs.integration.github

import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File

@ConfigurationProperties("klibs.integration.github")
data class GitHubIntegrationProperties(
    val personalAccessToken: String? = null,
    val cache: Cache,
) {
    data class Cache(
        val requestCachePath: File? = null,
        val requestCacheSizeMb: Int? = null,
    )
}
