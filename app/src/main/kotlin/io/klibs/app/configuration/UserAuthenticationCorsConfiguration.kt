package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
class UserAuthenticationCorsConfiguration {

    @Bean
    fun authenticationCorsConfigurationSource(
        userAuthenticationProperties: UserAuthenticationProperties,
    ): UrlBasedCorsConfigurationSource {
        val trustedOrigin = userAuthenticationProperties.trustedFrontendOrigin
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
