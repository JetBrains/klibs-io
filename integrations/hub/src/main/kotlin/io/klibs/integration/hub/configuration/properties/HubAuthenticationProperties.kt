package io.klibs.integration.hub.configuration.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "klibs.auth.hub")
class HubAuthenticationProperties(
    /** Hub base URL, including its context path, for example http://localhost:7777/hub. */
    @field:NotBlank(message = "KLIBS_AUTH_HUB_BASE_URL must be configured when Hub authentication is enabled")
    val baseUrl: String,

    /** Client ID from the klibs.io service registered in Hub. */
    @field:NotBlank(message = "KLIBS_AUTH_HUB_CLIENT_ID must be configured when Hub authentication is enabled")
    val clientId: String,

    /**
     * Space-separated OAuth scopes. Login reads only the Hub user ID; profile and email are requested for planned
     * features and are not stored.
     */
    @field:NotBlank(message = "KLIBS_AUTH_HUB_SCOPE must not be blank")
    val scope: String,

    /** Backend callback URL registered in Hub, rather than the frontend destination after login. */
    @field:NotBlank(message = "KLIBS_AUTH_HUB_CALLBACK_URL must be configured when Hub authentication is enabled")
    val callbackUrl: String,

    /** Maximum duration of each token or user-info request to Hub. */
    val requestTimeout: Duration,

    /** Lifetime shared by signed OAuth state and its browser login cookie, not the local user session. */
    val loginAttemptTtl: Duration,
) {
    companion object {
        const val ENABLED_PROPERTY = "klibs.auth.hub.enabled"
    }
}
