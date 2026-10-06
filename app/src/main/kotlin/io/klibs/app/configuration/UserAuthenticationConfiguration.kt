package io.klibs.app.configuration

import io.klibs.app.auth.TrustedOriginValidator
import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.core.user.service.UserSessionSettings
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.Base64

@Configuration
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
@EnableConfigurationProperties(UserAuthenticationProperties::class)
class UserAuthenticationConfiguration {
    @Bean
    fun authenticationHashingService(
        userAuthenticationProperties: UserAuthenticationProperties,
    ): AuthenticationHashingService {
        val secret = try {
            Base64.getDecoder().decode(userAuthenticationProperties.hmacSecret)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("KLIBS_AUTH_HMAC_SECRET must be valid Base64", exception)
        }
        require(secret.size >= MINIMUM_HMAC_SECRET_SIZE_BYTES) {
            "KLIBS_AUTH_HMAC_SECRET must contain at least $MINIMUM_HMAC_SECRET_SIZE_BYTES bytes"
        }

        return AuthenticationHashingService(secret)
    }

    @Bean
    fun userSessionSettings(
        userAuthenticationProperties: UserAuthenticationProperties,
    ): UserSessionSettings = UserSessionSettings(
        idleTtl = userAuthenticationProperties.session.idleTtl,
        refreshInterval = userAuthenticationProperties.session.refreshInterval,
        absoluteTtl = userAuthenticationProperties.session.absoluteTtl,
    )

    @Bean
    fun trustedOriginValidator(
        userAuthenticationProperties: UserAuthenticationProperties,
        environment: Environment,
    ): TrustedOriginValidator = TrustedOriginValidator(
        trustedOrigin = userAuthenticationProperties.trustedFrontendOrigin,
        requireHttps = environment.matchesProfiles("prod"),
    )

    private companion object {
        const val MINIMUM_HMAC_SECRET_SIZE_BYTES = 32
    }
}
