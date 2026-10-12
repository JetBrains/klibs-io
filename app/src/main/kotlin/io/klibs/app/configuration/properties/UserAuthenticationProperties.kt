package io.klibs.app.configuration.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "klibs.auth")
data class UserAuthenticationProperties(
    /**
     * Base64-encoded key with at least 32 decoded bytes, used for:
     * Hub identity hashes, session-token hashes, and OAuth-state signatures.
     */
    @field:NotBlank(message = "KLIBS_AUTH_HMAC_SECRET must be configured when Hub authentication is enabled")
    val hmacSecret: String,

    /** Exact frontend origin allowed for credentialed CORS and session-protected writes. */
    @field:NotBlank(message = "KLIBS_AUTH_TRUSTED_FRONTEND_ORIGIN must be configured when Hub authentication is enabled")
    val trustedFrontendOrigin: String,

    @field:Valid
    val session: Session,
) {
    data class Session(
        /** Session expires after this much inactivity; accepted activity may extend it up to the absolute TTL. */
        val idleTtl: Duration,

        /** Minimum time between extensions of an active session. */
        val refreshInterval: Duration,

        /** Session expires this long after creation regardless of activity and cannot be extended further. */
        val absoluteTtl: Duration,

        /** Session cookie name; production requires the `__Host-` prefix. */
        @field:NotBlank(message = "KLIBS_AUTH_SESSION_COOKIE_NAME must not be blank")
        val cookieName: String,

        /** Whether the cookie is HTTPS-only; this must be true in production. */
        val cookieSecure: Boolean,
    )
}
