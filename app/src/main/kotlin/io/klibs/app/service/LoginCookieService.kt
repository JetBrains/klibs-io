package io.klibs.app.service

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.dto.OAuthLoginAttemptDto
import io.klibs.core.user.dto.OAuthLoginCallbackDto
import io.klibs.core.user.service.OAuthLoginAttemptService
import io.klibs.core.user.service.OAuthLoginSettings
import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties.Companion.ENABLED_PROPERTY
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(ENABLED_PROPERTY, havingValue = "true")
class LoginCookieService(
    userAuthenticationProperties: UserAuthenticationProperties,
    private val loginSettings: OAuthLoginSettings,
    private val loginAttemptService: OAuthLoginAttemptService,
    private val sessionCookieService: SessionCookieService,
) {
    // Inherit the validated __Host- prefix and Secure policy while keeping login and session cookies separate.
    private val cookieName = "${userAuthenticationProperties.session.cookieName}_login"

    fun createLoginCookie(attempt: OAuthLoginAttemptDto): ResponseCookie =
        sessionCookieService.createAuthenticationCookie(
            cookieName,
            "${attempt.state}.${attempt.codeVerifier}",
            loginSettings.attemptTtl,
        )

    fun validateAndConsumeLoginAttempt(
        request: HttpServletRequest,
        response: HttpServletResponse,
        state: String?,
    ): OAuthLoginCallbackDto? {
        val attempt = readLoginAttempt(request)
        if (loginAttemptService.matchesBrowserAttempt(state, attempt)) {
            // Set cleanup before continuing: a later validation or Hub error must still consume the matching cookie.
            val deletionCookie = sessionCookieService.createAuthenticationCookie(cookieName, "", Duration.ZERO)
            response.addHeader(HttpHeaders.SET_COOKIE, deletionCookie.toString())
        }
        return loginAttemptService.validateLoginAttempt(state, attempt)
    }

    private fun readLoginAttempt(request: HttpServletRequest): OAuthLoginAttemptDto? {
        val value = request.cookies?.filter { it.name == cookieName }?.singleOrNull()?.value ?: return null
        val separator = value.lastIndexOf('.')
        if (separator <= 0 || separator == value.lastIndex) return null
        return OAuthLoginAttemptDto(value.substring(0, separator), value.substring(separator + 1))
    }
}
