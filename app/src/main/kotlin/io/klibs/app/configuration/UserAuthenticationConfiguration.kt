package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.core.user.service.UserSessionSettings
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    ): UserSessionSettings {
        val idleTtlFromProperty = userAuthenticationProperties.session.idleTtl
        val refreshIntervalFromProperty = userAuthenticationProperties.session.refreshInterval
        val absoluteTtlFromProperty = userAuthenticationProperties.session.absoluteTtl
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
    }
}
