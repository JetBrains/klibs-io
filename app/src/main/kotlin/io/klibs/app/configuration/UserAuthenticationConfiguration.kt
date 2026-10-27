package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.core.user.service.UserSessionSettings
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.URI
import java.util.Base64

@Configuration
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
@EnableConfigurationProperties(UserAuthenticationProperties::class)
class UserAuthenticationConfiguration(
    private val properties: UserAuthenticationProperties,
    environment: Environment,
) {

    @Bean
    fun authenticationHashingService(): AuthenticationHashingService {
        val secret = try {
            Base64.getDecoder().decode(properties.hmacSecret)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("KLIBS_AUTH_HMAC_SECRET must be valid Base64", exception)
        }
        require(secret.size >= MINIMUM_HMAC_SECRET_SIZE_BYTES) {
            "KLIBS_AUTH_HMAC_SECRET must contain at least $MINIMUM_HMAC_SECRET_SIZE_BYTES bytes"
        }

        return AuthenticationHashingService(secret)
    }

    @Bean
    fun userSessionSettings(): UserSessionSettings {
        val idleTtlFromProperty = properties.session.idleTtl
        val refreshIntervalFromProperty = properties.session.refreshInterval
        val absoluteTtlFromProperty = properties.session.absoluteTtl
        require(idleTtlFromProperty.isPositive) {
            "Session idle TTL must be positive"
        }
        require(refreshIntervalFromProperty.isPositive && refreshIntervalFromProperty < idleTtlFromProperty) {
            "Session refresh interval must be positive and shorter than the idle TTL"
        }
        require(absoluteTtlFromProperty >= idleTtlFromProperty) {
            "Session absolute TTL must be greater than or equal to the idle TTL"
        }
        return UserSessionSettings(
            idleTtl = idleTtlFromProperty,
            refreshInterval = refreshIntervalFromProperty,
            absoluteTtl = absoluteTtlFromProperty,
        )
    }

    private companion object {
        const val MINIMUM_HMAC_SECRET_SIZE_BYTES = 32
        private const val HOST_COOKIE_PREFIX = "__Host-"
        private val COOKIE_NAME_PATTERN = Regex("[!#\$%&'*+.^_`|~0-9A-Za-z-]+")
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

    private fun validateCookieSettings(
        session: UserAuthenticationProperties.Session,
        production: Boolean,
    ) {
        require(COOKIE_NAME_PATTERN.matches(session.cookieName)) {
            "KLIBS_AUTH_SESSION_COOKIE_NAME contains invalid characters"
        }
        require(!production || session.cookieSecure) {
            "KLIBS_AUTH_SESSION_COOKIE_SECURE must be true in production"
        }
        require(!production || session.cookieName.startsWith(HOST_COOKIE_PREFIX)) {
            "KLIBS_AUTH_SESSION_COOKIE_NAME must start with $HOST_COOKIE_PREFIX in production"
        }
    }

    init {
        val production = environment.matchesProfiles("prod")
        validateTrustedOrigin(properties.trustedFrontendOrigin, production)
        validateCookieSettings(properties.session, production)
    }
}
