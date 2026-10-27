package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserAuthenticationConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = ["", "not-base64", SHORT_HMAC_SECRET])
    fun `rejects an invalid hmac secret`(hmacSecret: String) {
        val configuration = createConfiguration(properties(hmacSecret = hmacSecret))

        assertFailsWith<IllegalArgumentException> {
            configuration.authenticationHashingService()
        }
    }

    @Test
    fun `maps session properties to core settings`() {
        val idleTtl = Duration.ofHours(17)
        val refreshInterval = Duration.ofMinutes(43)
        val absoluteTtl = Duration.ofDays(9)
        val properties = properties(
            session = SESSION_PROPERTIES.copy(
                idleTtl = idleTtl,
                refreshInterval = refreshInterval,
                absoluteTtl = absoluteTtl,
            ),
        )

        val settings = createConfiguration(properties).userSessionSettings()

        assertEquals(idleTtl, settings.idleTtl)
        assertEquals(refreshInterval, settings.refreshInterval)
        assertEquals(absoluteTtl, settings.absoluteTtl)
    }

    @ParameterizedTest
    @EnumSource(InvalidSessionLifetime::class)
    fun `rejects invalid session lifetime configuration`(
        configuration: InvalidSessionLifetime,
    ) {
        val properties = properties(
            session = SESSION_PROPERTIES.copy(
                idleTtl = configuration.idleTtl,
                refreshInterval = configuration.refreshInterval,
                absoluteTtl = configuration.absoluteTtl,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties).userSessionSettings()
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "file://frontend.example",
            "https:/frontend.example",
            "https://user:password@frontend.example",
            "https://frontend.example/",
            "https://frontend.example?tab=security",
            "https://frontend.example#account",
            "https://frontend example",
        ]
    )
    fun `rejects an invalid trusted origin`(origin: String) {
        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties(trustedOrigin = origin))
        }
    }

    @Test
    fun `requires a secure trusted origin in production`() {
        val properties = properties(
            trustedOrigin = "http://frontend.example",
            session = SESSION_PROPERTIES.copy(cookieName = PRODUCTION_SESSION_COOKIE_NAME),
        )

        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties, "prod")
        }
    }

    @Test
    fun `rejects an insecure cookie in production`() {
        val session = SESSION_PROPERTIES.copy(
            cookieName = PRODUCTION_SESSION_COOKIE_NAME,
            cookieSecure = false,
        )

        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties(session = session), "prod")
        }
    }

    @Test
    fun `rejects a session cookie without the host prefix in production`() {
        val session = SESSION_PROPERTIES.copy(
            cookieName = SESSION_COOKIE_NAME,
            cookieSecure = true,
        )

        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties(session = session), "prod")
        }
    }

    @Test
    fun `rejects an invalid session cookie name`() {
        val session = SESSION_PROPERTIES.copy(cookieName = "invalid cookie name")

        assertFailsWith<IllegalArgumentException> {
            createConfiguration(properties(session = session))
        }
    }

    private fun properties(
        hmacSecret: String = VALID_HMAC_SECRET,
        trustedOrigin: String = TRUSTED_ORIGIN,
        session: UserAuthenticationProperties.Session = SESSION_PROPERTIES,
    ) = UserAuthenticationProperties(
        hmacSecret = hmacSecret,
        trustedFrontendOrigin = trustedOrigin,
        session = session,
    )

    private fun createConfiguration(
        properties: UserAuthenticationProperties,
        vararg profiles: String,
    ): UserAuthenticationConfiguration {
        val environment = MockEnvironment().apply {
            setActiveProfiles(*profiles)
        }
        return UserAuthenticationConfiguration(properties, environment)
    }

    private companion object {
        const val VALID_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        const val SHORT_HMAC_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
        const val TRUSTED_ORIGIN = "https://frontend.example"
        const val PRODUCTION_SESSION_COOKIE_NAME = "__Host-klibs_session"
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
