package io.klibs.app.configuration.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "klibs.auth")
data class UserAuthenticationProperties(
    @field:NotBlank(message = "KLIBS_AUTH_HMAC_SECRET must be configured when Hub authentication is enabled")
    val hmacSecret: String,

    @field:NotBlank(message = "KLIBS_AUTH_TRUSTED_FRONTEND_ORIGIN must be configured when Hub authentication is enabled")
    val trustedFrontendOrigin: String,

    @field:Valid
    val session: Session = Session(),
) {
    data class Session(
        /** Maximum time a session may remain unused before it expires. */
        val idleTtl: Duration = Duration.ofDays(30),

        /** Minimum time between extensions of an active session. */
        val refreshInterval: Duration = Duration.ofDays(1),

        /** Maximum session lifetime measured from its creation time. */
        val absoluteTtl: Duration = Duration.ofDays(180),

        @field:NotBlank(message = "KLIBS_AUTH_SESSION_COOKIE_NAME must not be blank")
        val cookieName: String = "klibs_session",

        val cookieSecure: Boolean = true,
    )
}
