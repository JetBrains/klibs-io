package io.klibs.app.configuration

import io.klibs.app.configuration.properties.AuthProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserAuthenticationConfigurationTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["not-base64", SHORT_HMAC_SECRET])
    fun `rejects an invalid hmac secret`(hmacSecret: String?) {
        val configuration = configuration()

        assertFailsWith<IllegalArgumentException> {
            configuration.authenticationHashingService(AuthProperties(hmacSecret = hmacSecret))
        }
    }

    @Test
    fun `allows an insecure cookie outside production`() {
        val cookieService = configuration("local").sessionCookieService(
            AuthProperties(
                session = AuthProperties.Session(
                    cookieName = LOCAL_SESSION_COOKIE_NAME,
                    cookieSecure = false,
                )
            )
        )

        assertFalse(cookieService.createSessionCookie("token").isSecure)
    }

    @Test
    fun `rejects an insecure cookie in production`() {
        assertFailsWith<IllegalArgumentException> {
            configuration("prod").sessionCookieService(
                AuthProperties(
                    session = AuthProperties.Session(
                        cookieName = PRODUCTION_SESSION_COOKIE_NAME,
                        cookieSecure = false,
                    )
                )
            )
        }
    }

    @Test
    fun `rejects a session cookie without the host prefix in production`() {
        assertFailsWith<IllegalArgumentException> {
            configuration("prod").sessionCookieService(
                AuthProperties(
                    session = AuthProperties.Session(
                        cookieName = LOCAL_SESSION_COOKIE_NAME,
                        cookieSecure = true,
                    )
                )
            )
        }
    }

    @Test
    fun `rejects an invalid host-prefixed session cookie name during configuration`() {
        assertFailsWith<IllegalArgumentException> {
            configuration("prod").sessionCookieService(
                AuthProperties(
                    session = AuthProperties.Session(
                        cookieName = "__Host-invalid cookie name",
                        cookieSecure = true,
                    )
                )
            )
        }
    }

    @Test
    fun `creates a host-prefixed session cookie in production`() {
        val cookieService = configuration("prod").sessionCookieService(
            AuthProperties(
                session = AuthProperties.Session(
                    cookieName = PRODUCTION_SESSION_COOKIE_NAME,
                    cookieSecure = true,
                )
            )
        )

        val cookie = cookieService.createSessionCookie("token")

        assertEquals(PRODUCTION_SESSION_COOKIE_NAME, cookie.name)
        assertEquals("/", cookie.path)
        assertNull(cookie.domain)
        assertTrue(cookie.isSecure)
    }

    @Test
    fun `rejects a non-https trusted origin in production`() {
        assertFailsWith<IllegalArgumentException> {
            configuration("prod").trustedOriginValidator(
                AuthProperties(trustedFrontendOrigin = "http://frontend.example")
            )
        }
    }

    private fun configuration(vararg profiles: String): UserAuthenticationConfiguration {
        val environment = MockEnvironment()
        environment.setActiveProfiles(*profiles)
        return UserAuthenticationConfiguration(environment)
    }

    private companion object {
        const val LOCAL_SESSION_COOKIE_NAME = "klibs_session"
        const val PRODUCTION_SESSION_COOKIE_NAME = "__Host-klibs_session"
        const val SHORT_HMAC_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
    }
}
