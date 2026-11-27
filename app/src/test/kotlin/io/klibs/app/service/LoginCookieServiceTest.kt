package io.klibs.app.service

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.dto.OAuthLoginAttemptDto
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.core.user.service.FrontendRedirectService
import io.klibs.core.user.service.OAuthLoginAttemptService
import io.klibs.core.user.service.OAuthLoginSettings
import io.klibs.core.user.service.PkceService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockCookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginCookieServiceTest {
    private val settings = OAuthLoginSettings(Duration.ofMinutes(7), "https://frontend.example")
    private val attemptService = createAttemptService(NOW)
    private val uut = createCookieService(attemptService)

    @Test
    fun `creates a short-lived host-scoped HttpOnly login cookie`() {
        val attempt = attemptService.createLoginAttempt("/")

        val cookie = uut.createLoginCookie(attempt)

        assertEquals(LOGIN_COOKIE_NAME, cookie.name)
        assertEquals(settings.attemptTtl, cookie.maxAge)
        assertEquals("/", cookie.path)
        assertEquals("Lax", cookie.sameSite)
        assertNull(cookie.domain)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.isSecure)
    }

    @Test
    fun `validates the attempt stored in the browser cookie and consumes it`() {
        val attempt = attemptService.createLoginAttempt("/project/JetBrains/kotlin")
        val request = requestWith(attempt)
        val response = MockHttpServletResponse()

        val validated = assertNotNull(uut.validateAndConsumeLoginAttempt(request, response, attempt.state))

        assertEquals(attempt.codeVerifier, validated.codeVerifier)
        assertEquals("https://frontend.example/project/JetBrains/kotlin", validated.redirectUrl)
        val deletion = MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE)))
        assertEquals(LOGIN_COOKIE_NAME, deletion.name)
        assertEquals("", deletion.value)
        assertEquals(0, deletion.maxAge)
        assertEquals("/", deletion.path)
        assertEquals("Lax", deletion.sameSite)
        assertNull(deletion.domain)
        assertTrue(deletion.isHttpOnly)
        assertTrue(deletion.secure)
    }

    @Test
    fun `callback without a cookie fails without setting any cookie`() {
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(MockHttpServletRequest(), response, "state"))
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE))
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["unrelated-state"])
    fun `unmatched callback leaves the browser attempt intact`(state: String?) {
        val attempt = attemptService.createLoginAttempt("/")
        val request = requestWith(attempt)
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(request, response, state))
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE))
        assertNotNull(uut.validateAndConsumeLoginAttempt(request, MockHttpServletResponse(), attempt.state))
    }

    @Test
    fun `expired matching attempt is rejected and its cookie is consumed`() {
        val attempt = attemptService.createLoginAttempt("/")
        val uut = createCookieService(createAttemptService(NOW.plus(settings.attemptTtl)))
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(requestWith(attempt), response, attempt.state))
        assertEquals(0, MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE))).maxAge)
    }

    @Test
    fun `matching state with a substituted verifier is rejected and consumes its cookie`() {
        val attempt = attemptService.createLoginAttempt("/")
        val changed = OAuthLoginAttemptDto(attempt.state, PkceService().createVerifier())
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(requestWith(changed), response, attempt.state))
        assertEquals(0, MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE))).maxAge)
    }

    @Test
    fun `matching state with a bad signature is rejected and consumes its cookie`() {
        val original = attemptService.createLoginAttempt("/")
        val attempt = OAuthLoginAttemptDto(original.state + "x", original.codeVerifier)
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(requestWith(attempt), response, attempt.state))
        assertEquals(0, MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE))).maxAge)
    }

    @Test
    fun `cleanup preserves other response cookies`() {
        val attempt = attemptService.createLoginAttempt("/")
        val response = MockHttpServletResponse().apply {
            addHeader(HttpHeaders.SET_COOKIE, "unrelated=preserved; Path=/")
        }

        assertNotNull(uut.validateAndConsumeLoginAttempt(requestWith(attempt), response, attempt.state))

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        assertEquals(2, cookies.size)
        assertEquals("unrelated=preserved; Path=/", cookies.first())
        assertEquals(LOGIN_COOKIE_NAME, MockCookie.parse(cookies.last()).name)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "malformed", ".verifier", "state."])
    fun `malformed cookie is rejected without cookie mutation`(value: String) {
        val request = MockHttpServletRequest().apply { setCookies(Cookie(LOGIN_COOKIE_NAME, value)) }
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(request, response, "state"))
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `duplicate login cookies are rejected without selecting an ambiguous attempt`() {
        val attempt = attemptService.createLoginAttempt("/")
        val value = uut.createLoginCookie(attempt).value
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie(LOGIN_COOKIE_NAME, value), Cookie(LOGIN_COOKIE_NAME, value))
        }
        val response = MockHttpServletResponse()

        assertNull(uut.validateAndConsumeLoginAttempt(request, response, attempt.state))
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `local login cookie inherits the insecure session setting`() {
        val uut = createCookieService(attemptService, "klibs_session", false)
        val attempt = attemptService.createLoginAttempt("/")

        val cookie = uut.createLoginCookie(attempt)

        assertEquals("klibs_session_login", cookie.name)
        assertFalse(cookie.isSecure)
    }

    @Test
    fun `cookie with the longest allowed returnTo fits the browser cookie size limit`() {
        val cookie = uut.createLoginCookie(attemptService.createLoginAttempt("/" + "a".repeat(1023)))

        assertTrue(cookie.toString().toByteArray(Charsets.UTF_8).size < 4096)
    }

    private fun requestWith(attempt: OAuthLoginAttemptDto) = MockHttpServletRequest().apply {
        setCookies(Cookie(LOGIN_COOKIE_NAME, uut.createLoginCookie(attempt).value))
    }

    private fun createAttemptService(now: Instant) = OAuthLoginAttemptService(
        AuthenticationHashingService("0123456789abcdef0123456789abcdef".toByteArray()),
        PkceService(),
        FrontendRedirectService(),
        settings,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun createCookieService(
        attemptService: OAuthLoginAttemptService,
        cookieName: String = SESSION_COOKIE_NAME,
        secure: Boolean = true,
    ): LoginCookieService {
        val properties = UserAuthenticationProperties(
            hmacSecret = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            trustedFrontendOrigin = settings.trustedFrontendOrigin,
            session = UserAuthenticationProperties.Session(
                idleTtl = Duration.ofDays(30),
                refreshInterval = Duration.ofDays(1),
                absoluteTtl = Duration.ofDays(180),
                cookieName = cookieName,
                cookieSecure = secure,
            ),
        )
        return LoginCookieService(properties, settings, attemptService, SessionCookieService(properties))
    }

    private companion object {
        const val SESSION_COOKIE_NAME = "__Host-test_session"
        const val LOGIN_COOKIE_NAME = "${SESSION_COOKIE_NAME}_login"
        val NOW: Instant = Instant.parse("2026-11-09T10:00:00Z")
    }
}
