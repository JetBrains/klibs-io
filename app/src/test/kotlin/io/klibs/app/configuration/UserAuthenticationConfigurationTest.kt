package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
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
            ),
        )

        val settings = configuration.userSessionSettings(properties)

        assertEquals(IDLE_TTL, settings.idleTtl)
        assertEquals(REFRESH_INTERVAL, settings.refreshInterval)
        assertEquals(ABSOLUTE_TTL, settings.absoluteTtl)
    }

    @Test
    fun `requires a secure trusted origin in production`() {
        val configuration = UserAuthenticationConfiguration()
        val properties = UserAuthenticationProperties(
            hmacSecret = VALID_HMAC_SECRET,
            trustedFrontendOrigin = "http://frontend.example",
        )
        val environment = MockEnvironment().apply {
            setActiveProfiles("prod")
        }

        assertFailsWith<IllegalArgumentException> {
            configuration.trustedOriginValidator(properties, environment)
        }
    }

    private companion object {
        const val VALID_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        const val SHORT_HMAC_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
        const val TRUSTED_ORIGIN = "https://frontend.example"
        val IDLE_TTL: Duration = Duration.ofDays(30)
        val REFRESH_INTERVAL: Duration = Duration.ofDays(1)
        val ABSOLUTE_TTL: Duration = Duration.ofDays(180)
    }
}
