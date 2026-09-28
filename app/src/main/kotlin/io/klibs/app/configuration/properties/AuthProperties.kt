package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "klibs.auth")
data class AuthProperties(
    val users: List<User> = emptyList(),

    val enabled: Boolean = false,
    val hmacSecret: String? = null,
    val trustedFrontendOrigin: String? = null,
    val session: Session = Session(),
) {
    data class Session(
        val idleTtl: Duration = Duration.ofDays(30),
        val refreshInterval: Duration = Duration.ofDays(1),
        val absoluteTtl: Duration = Duration.ofDays(180),
        val cookieName: String = "klibs_session",
        val cookieSecure: Boolean = true,
    )

    data class User(
        val username: String,
        val password: String,
        val roles: List<String>
    )
}
