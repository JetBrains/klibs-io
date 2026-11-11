package io.klibs.app.service

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
class SessionCookieService(
    userAuthenticationProperties: UserAuthenticationProperties,
) {
    private val sessionProperties = userAuthenticationProperties.session

    fun getSessionToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == sessionProperties.cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }

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

    private companion object {
        const val SAME_SITE = "Lax"
        const val COOKIE_PATH = "/"
    }
}
