package io.klibs.app.auth

import io.klibs.app.configuration.properties.AuthProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionCookieServiceTest {
    private val sessionProperties = AuthProperties.Session(
        idleTtl = Duration.ofHours(12),
        cookieName = SESSION_COOKIE_NAME,
        cookieSecure = true,
    )
    private val service = SessionCookieService(sessionProperties)

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

    private companion object {
        const val SESSION_COOKIE_NAME = "test_session"
    }
}
