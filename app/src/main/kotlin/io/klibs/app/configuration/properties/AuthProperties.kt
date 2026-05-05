package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "klibs.auth")
data class AuthProperties(
    val users: List<User> = emptyList(),
    val github: GitHub = GitHub(),
    val jwt: Jwt = Jwt()
) {
    data class User(
        val username: String,
        val password: String,
        val roles: List<String>
    )

    data class GitHub(
        val clientId: String = "",
        val clientSecret: String = ""
    )

    data class Jwt(
        val secret: String = ""
    )
}
