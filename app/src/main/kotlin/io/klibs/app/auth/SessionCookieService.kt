package io.klibs.app.auth

import io.klibs.app.configuration.properties.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import java.time.Duration

class SessionCookieService(
    private val sessionProperties: AuthProperties.Session,
) {
    init {
        validateCookieName(sessionProperties.cookieName)
    }

    fun getSessionToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == sessionProperties.cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }

    fun createSessionCookie(token: String): ResponseCookie =
        createSessionCookie(token, sessionProperties.idleTtl)

    fun createSessionCookie(token: String, maxAge: Duration): ResponseCookie {
        require(token.isNotBlank()) { "Session token must not be blank" }
        require(maxAge.isPositive) { "Session cookie max age must be positive" }

        return createResponseCookie(token, maxAge)
    }

    fun createSessionCookieForDeletion(): ResponseCookie {
        // A server removes a browser cookie by replacing it with the same scoped cookie and Max-Age=0.
        return createResponseCookie("", Duration.ZERO)
    }

    private fun createResponseCookie(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(sessionProperties.cookieName, value)
            .httpOnly(true)
            .secure(sessionProperties.cookieSecure)
            .sameSite(SAME_SITE)
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build()

    private fun validateCookieName(cookieName: String) {
        ResponseCookie.from(cookieName, "").build()
    }

    private companion object {
        const val SAME_SITE = "Lax"
        const val COOKIE_PATH = "/"
    }
}
