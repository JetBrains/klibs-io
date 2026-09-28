package io.klibs.app.configuration

import io.klibs.app.auth.SessionCookieService
import io.klibs.app.auth.TrustedOriginValidator
import io.klibs.app.configuration.properties.AuthProperties
import io.klibs.core.user.repository.KlibsUserRepository
import io.klibs.core.user.repository.KlibsUserSessionRepository
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.core.user.service.UserService
import io.klibs.core.user.service.UserSessionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.Base64

@Configuration
@ConditionalOnProperty("klibs.auth.enabled", havingValue = "true")
class UserAuthenticationConfiguration(
    private val environment: Environment,
) {
    @Bean
    fun authenticationHashingService(authProperties: AuthProperties): AuthenticationHashingService {
        val encodedSecret = authProperties.hmacSecret
        if (encodedSecret.isNullOrBlank()) {
            throw IllegalArgumentException(
                "KLIBS_AUTH_HMAC_SECRET must be configured when authentication is enabled"
            )
        }

        val secret = try {
            Base64.getDecoder().decode(encodedSecret)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("KLIBS_AUTH_HMAC_SECRET must be valid Base64", exception)
        }
        require(secret.size >= MINIMUM_HMAC_SECRET_SIZE_BYTES) {
            "KLIBS_AUTH_HMAC_SECRET must contain at least $MINIMUM_HMAC_SECRET_SIZE_BYTES bytes"
        }

        return AuthenticationHashingService(secret)
    }

    @Bean
    fun userService(
        userRepository: KlibsUserRepository,
        hashingService: AuthenticationHashingService,
    ): UserService = UserService(userRepository, hashingService)

    @Bean
    fun userSessionService(
        sessionRepository: KlibsUserSessionRepository,
        hashingService: AuthenticationHashingService,
        authProperties: AuthProperties,
    ): UserSessionService = UserSessionService(
        sessionRepository = sessionRepository,
        hashingService = hashingService,
        sessionIdleTtl = authProperties.session.idleTtl,
        sessionRefreshInterval = authProperties.session.refreshInterval,
        sessionAbsoluteTtl = authProperties.session.absoluteTtl,
    )

    @Bean
    fun sessionCookieService(authProperties: AuthProperties): SessionCookieService {
        if (environment.matchesProfiles("prod")) {
            require(authProperties.session.cookieSecure) {
                "KLIBS_AUTH_SESSION_COOKIE_SECURE must be true in production"
            }
            require(authProperties.session.cookieName.startsWith(HOST_COOKIE_PREFIX)) {
                "KLIBS_AUTH_SESSION_COOKIE_NAME must start with $HOST_COOKIE_PREFIX in production"
            }
        }

        return SessionCookieService(authProperties.session)
    }

    @Bean
    fun trustedOriginValidator(authProperties: AuthProperties): TrustedOriginValidator {
        val trustedOrigin = authProperties.trustedFrontendOrigin
        if (trustedOrigin.isNullOrBlank()) {
            throw IllegalArgumentException(
                "KLIBS_AUTH_TRUSTED_FRONTEND_ORIGIN must be configured when authentication is enabled"
            )
        }

        return TrustedOriginValidator(
            trustedOrigin = trustedOrigin,
            requireHttps = environment.matchesProfiles("prod"),
        )
    }

    private companion object {
        const val HOST_COOKIE_PREFIX = "__Host-"
        const val MINIMUM_HMAC_SECRET_SIZE_BYTES = 32
    }
}
