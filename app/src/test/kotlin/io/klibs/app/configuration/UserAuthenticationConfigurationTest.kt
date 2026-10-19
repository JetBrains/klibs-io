package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserAuthenticationConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = ["", "not-base64", SHORT_HMAC_SECRET])
    fun `rejects an invalid hmac secret`(hmacSecret: String) {
        val configuration = UserAuthenticationConfiguration()

        assertFailsWith<IllegalArgumentException> {
            configuration.authenticationHashingService(
                UserAuthenticationProperties(
                    hmacSecret = hmacSecret,
                    trustedFrontendOrigin = TRUSTED_ORIGIN,
                    session = SESSION_PROPERTIES,
                )
            )
        }
    }

    @Test
    fun `maps session properties to core settings`() {
        val configuration = UserAuthenticationConfiguration()
        val properties = UserAuthenticationProperties(
            hmacSecret = VALID_HMAC_SECRET,
            trustedFrontendOrigin = TRUSTED_ORIGIN,
            session = UserAuthenticationProperties.Session(
                idleTtl = IDLE_TTL,
                refreshInterval = REFRESH_INTERVAL,
                absoluteTtl = ABSOLUTE_TTL,
                cookieName = SESSION_COOKIE_NAME,
                cookieSecure = true,
            ),
        )

        val settings = configuration.userSessionSettings(properties)

        assertEquals(IDLE_TTL, settings.idleTtl)
        assertEquals(REFRESH_INTERVAL, settings.refreshInterval)
        assertEquals(ABSOLUTE_TTL, settings.absoluteTtl)
    }

    @ParameterizedTest
    @EnumSource(InvalidSessionLifetime::class)
    fun `rejects invalid session lifetime configuration`(
        configuration: InvalidSessionLifetime,
    ) {
        val properties = UserAuthenticationProperties(
            hmacSecret = VALID_HMAC_SECRET,
            trustedFrontendOrigin = TRUSTED_ORIGIN,
            session = SESSION_PROPERTIES.copy(
                idleTtl = configuration.idleTtl,
                refreshInterval = configuration.refreshInterval,
                absoluteTtl = configuration.absoluteTtl,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            UserAuthenticationConfiguration().userSessionSettings(properties)
        }
    }

    private companion object {
        const val VALID_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        const val SHORT_HMAC_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
        const val TRUSTED_ORIGIN = "https://frontend.example"
        val IDLE_TTL: Duration = Duration.ofDays(30)
        val REFRESH_INTERVAL: Duration = Duration.ofDays(1)
        val ABSOLUTE_TTL: Duration = Duration.ofDays(180)
        const val SESSION_COOKIE_NAME = "test_session"
        val SESSION_PROPERTIES = UserAuthenticationProperties.Session(
            idleTtl = IDLE_TTL,
            refreshInterval = REFRESH_INTERVAL,
            absoluteTtl = ABSOLUTE_TTL,
            cookieName = SESSION_COOKIE_NAME,
            cookieSecure = true,
        )
    }
}

enum class InvalidSessionLifetime(
    val idleTtl: Duration = Duration.ofDays(30),
    val refreshInterval: Duration = Duration.ofDays(1),
    val absoluteTtl: Duration = Duration.ofDays(180),
) {
    ZERO_IDLE_TTL(idleTtl = Duration.ZERO),
    ZERO_REFRESH_INTERVAL(refreshInterval = Duration.ZERO),
    REFRESH_INTERVAL_EQUAL_TO_IDLE(refreshInterval = Duration.ofDays(30)),
    ABSOLUTE_TTL_SHORTER_THAN_IDLE(absoluteTtl = Duration.ofDays(30).minusSeconds(1)),
}
