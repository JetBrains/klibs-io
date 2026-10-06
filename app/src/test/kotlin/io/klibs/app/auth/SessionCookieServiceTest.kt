package io.klibs.app.auth

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionCookieServiceTest {
    private val sessionProperties = UserAuthenticationProperties.Session(
        idleTtl = Duration.ofHours(12),
        cookieName = SESSION_COOKIE_NAME,
        cookieSecure = true,
    )
    private val service = createService(sessionProperties)

    @Test
    fun `reads the first matching session cookie value`() {
        val request = MockHttpServletRequest().apply {
            setCookies(
                Cookie("other", "ignored"),
                Cookie(SESSION_COOKIE_NAME, "first-token"),
                Cookie(SESSION_COOKIE_NAME, "second-token"),
            )
        }

        assertEquals("first-token", service.getSessionToken(request))
    }

    @Test
    fun `treats a missing or blank session cookie as unauthenticated`() {
        assertNull(service.getSessionToken(MockHttpServletRequest()))

        val requestWithBlankCookie = MockHttpServletRequest().apply {
            setCookies(Cookie(SESSION_COOKIE_NAME, ""))
        }
        assertNull(service.getSessionToken(requestWithBlankCookie))
    }

    @Test
    fun `creates a secure http-only session cookie`() {
        val cookie = service.createSessionCookie("session-token")

        assertEquals(SESSION_COOKIE_NAME, cookie.name)
        assertEquals("session-token", cookie.value)
        assertEquals("/", cookie.path)
        assertEquals("Lax", cookie.sameSite)
        assertEquals(sessionProperties.idleTtl, cookie.maxAge)
        assertNull(cookie.domain)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.isSecure)
    }

    @Test
    fun `creates a session cookie with the supplied remaining lifetime`() {
        val remainingTtl = Duration.ofMinutes(30)

        val cookie = service.createSessionCookie("session-token", remainingTtl)

        assertEquals(remainingTtl, cookie.maxAge)
    }

    @Test
    fun `creates a cookie for deletion with the same security attributes`() {
        val cookie = service.createSessionCookieForDeletion()

        assertEquals(SESSION_COOKIE_NAME, cookie.name)
        assertEquals("", cookie.value)
        assertEquals(Duration.ZERO, cookie.maxAge)
        assertEquals("/", cookie.path)
        assertEquals("Lax", cookie.sameSite)
        assertNull(cookie.domain)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.isSecure)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t", "\n"])
    fun `rejects a blank token when creating a live cookie`(token: String) {
        assertFailsWith<IllegalArgumentException> {
            service.createSessionCookie(token)
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1])
    fun `rejects a non-positive lifetime when creating a live cookie`(lifetimeSeconds: Long) {
        assertFailsWith<IllegalArgumentException> {
            service.createSessionCookie("session-token", Duration.ofSeconds(lifetimeSeconds))
        }
    }

    @Test
    fun `allows an insecure cookie outside production`() {
        val service = createService(
            UserAuthenticationProperties.Session(
                cookieName = LOCAL_SESSION_COOKIE_NAME,
                cookieSecure = false,
            ),
            "local",
        )

        assertFalse(service.createSessionCookie("token").isSecure)
    }

    @Test
    fun `rejects an insecure cookie in production`() {
        assertFailsWith<IllegalArgumentException> {
            createService(
                UserAuthenticationProperties.Session(
                    cookieName = PRODUCTION_SESSION_COOKIE_NAME,
                    cookieSecure = false,
                ),
                "prod",
            )
        }
    }

    @Test
    fun `rejects a session cookie without the host prefix in production`() {
        assertFailsWith<IllegalArgumentException> {
            createService(
                UserAuthenticationProperties.Session(
                    cookieName = LOCAL_SESSION_COOKIE_NAME,
                    cookieSecure = true,
                ),
                "prod",
            )
        }
    }

    @Test
    fun `rejects an invalid session cookie name`() {
        assertFailsWith<IllegalArgumentException> {
            createService(
                UserAuthenticationProperties.Session(
                    cookieName = "__Host-invalid cookie name",
                    cookieSecure = true,
                ),
                "prod",
            )
        }
    }

    @Test
    fun `creates a host-prefixed session cookie in production`() {
        val service = createService(
            UserAuthenticationProperties.Session(
                cookieName = PRODUCTION_SESSION_COOKIE_NAME,
                cookieSecure = true,
            ),
            "prod",
        )

        val cookie = service.createSessionCookie("token")

        assertEquals(PRODUCTION_SESSION_COOKIE_NAME, cookie.name)
        assertEquals("/", cookie.path)
        assertNull(cookie.domain)
        assertTrue(cookie.isSecure)
    }

    private fun createService(
        sessionProperties: UserAuthenticationProperties.Session,
        vararg profiles: String,
    ): SessionCookieService {
        val environment = MockEnvironment()
        environment.setActiveProfiles(*profiles)
        return SessionCookieService(
            UserAuthenticationProperties(
                hmacSecret = ENCODED_HMAC_SECRET,
                trustedFrontendOrigin = TRUSTED_ORIGIN,
                session = sessionProperties,
            ),
            environment,
        )
    }

    private companion object {
        const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        const val TRUSTED_ORIGIN = "https://frontend.example"
        const val SESSION_COOKIE_NAME = "test_session"
        const val LOCAL_SESSION_COOKIE_NAME = "klibs_session"
        const val PRODUCTION_SESSION_COOKIE_NAME = "__Host-klibs_session"
    }
}
