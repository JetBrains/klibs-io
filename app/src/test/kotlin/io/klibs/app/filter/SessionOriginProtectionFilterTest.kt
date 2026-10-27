package io.klibs.app.filter

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.app.service.SessionCookieService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import kotlin.test.assertEquals

class SessionOriginProtectionFilterTest {
    private val properties = UserAuthenticationProperties(
        hmacSecret = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        trustedFrontendOrigin = TRUSTED_ORIGIN,
        session = UserAuthenticationProperties.Session(
            idleTtl = Duration.ofDays(30),
            refreshInterval = Duration.ofDays(1),
            absoluteTtl = Duration.ofDays(180),
            cookieName = SESSION_COOKIE_NAME,
            cookieSecure = true,
        ),
    )
    private val filter = SessionOriginProtectionFilter(
        SessionCookieService(properties),
        properties.trustedFrontendOrigin,
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://other.example",
            "https://frontend.example:443",
            "https://frontend.example/",
        ]
    )
    fun `rejects an unsafe request with a session cookie and a non-matching origin`(origin: String?) {
        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus(origin))
    }

    private fun responseStatus(origin: String?): Int {
        val request = MockHttpServletRequest("POST", "/user/security-test").apply {
            setCookies(Cookie(SESSION_COOKIE_NAME, "session-token"))
            if (origin != null) addHeader(HttpHeaders.ORIGIN, origin)
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response) { _, servletResponse ->
            (servletResponse as HttpServletResponse).status = HttpServletResponse.SC_NO_CONTENT
        }

        return response.status
    }

    private companion object {
        const val TRUSTED_ORIGIN = "https://frontend.example"
        const val SESSION_COOKIE_NAME = "test_session"
    }
}
