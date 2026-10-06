package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "klibs.auth.basic")
data class BasicAuthenticationProperties(
    val users: List<BasicAuthUser> = emptyList(),
) {
    data class BasicAuthUser(
        val username: String,
        val password: String,
        val roles: List<String>,
    )
}
