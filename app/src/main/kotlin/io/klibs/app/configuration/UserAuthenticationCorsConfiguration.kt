package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.core.env.Environment
import java.net.URI

@Configuration
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
class UserAuthenticationCorsConfiguration {

    @Bean
    fun authenticationCorsConfigurationSource(
        userAuthenticationProperties: UserAuthenticationProperties,
        environment: Environment,
    ): UrlBasedCorsConfigurationSource {
        val trustedOrigin = userAuthenticationProperties.trustedFrontendOrigin
        validateTrustedOrigin(
            trustedOrigin,
            requireHttps = environment.matchesProfiles("prod")
        )
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration(
            AUTHENTICATION_ENDPOINTS_PATH,
            corsConfiguration(trustedOrigin, AUTHENTICATION_ALLOWED_METHODS),
        )
        source.registerCorsConfiguration(
            USER_ACTIONS_PATH,
            corsConfiguration(trustedOrigin, USER_ACTION_ALLOWED_METHODS),
        )
        return source
    }

    private fun validateTrustedOrigin(
        trustedOrigin: String,
        requireHttps: Boolean = false,
    ) {
        val uri = URI.create(trustedOrigin)
        require(uri.scheme == "http" || uri.scheme == "https") {
            "Trusted frontend origin must use HTTP or HTTPS"
        }
        require(!requireHttps || uri.scheme == "https") {
            "Trusted frontend origin must use HTTPS"
        }
        require(!uri.host.isNullOrBlank()) {
            "Trusted frontend origin must contain a host"
        }
        require(uri.userInfo == null && uri.path.isNullOrEmpty() && uri.query == null && uri.fragment == null) {
            "Trusted frontend origin must contain only scheme, host, and optional port"
        }
    }

    private fun corsConfiguration(
        trustedOrigin: String,
        allowedMethods: List<String>,
    ): CorsConfiguration = CorsConfiguration().apply {
        allowedOrigins = listOf(trustedOrigin)
        this.allowedMethods = allowedMethods
        allowedHeaders = listOf(CorsConfiguration.ALL)
        allowCredentials = true
    }

    private companion object {
        val AUTHENTICATION_ALLOWED_METHODS = listOf("GET", "POST", "OPTIONS")
        val USER_ACTION_ALLOWED_METHODS = listOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    }
}
