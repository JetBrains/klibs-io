package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "klibs.auth")
data class AuthProperties(
    val users: List<User> = emptyList()
) {
    data class User(
        val username: String,
        val password: String,
        val roles: List<String>
    )
}
