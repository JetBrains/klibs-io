package io.klibs.app.controller

import io.klibs.app.configuration.HUB_CALLBACK_PATH
import io.klibs.app.configuration.HUB_SIGN_IN_PATH
import io.klibs.app.service.LoginCookieService
import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.service.HubLoginService
import io.klibs.core.user.service.OAuthLoginAttemptService
import io.klibs.core.user.service.PkceService
import io.klibs.integration.hub.configuration.HUB_AUTHENTICATION_ENABLED_PROPERTY
import io.klibs.integration.hub.enums.HubAuthenticationFailure
import io.klibs.integration.hub.exception.HubAuthenticationException
import io.klibs.integration.hub.service.HubAuthenticationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration
import java.time.Instant

@RestController
@ConditionalOnProperty(HUB_AUTHENTICATION_ENABLED_PROPERTY, havingValue = "true")
class HubAuthenticationController(
    private val hubAuthenticationService: HubAuthenticationService,
    private val loginAttemptService: OAuthLoginAttemptService,
    private val loginCookieService: LoginCookieService,
    private val pkceService: PkceService,
    private val hubLoginService: HubLoginService,
    private val sessionCookieService: SessionCookieService,
) {
    @Operation(summary = "Start Hub sign-in", description = "Sets a temporary login cookie and redirects to Hub.")
    @GetMapping(HUB_SIGN_IN_PATH)
    fun signIn(
        @Schema(
            description = "Relative frontend path to return to after login; unsafe values use /",
            example = "/search",
        )
        @RequestParam(required = false) returnTo: String?,
    ): ResponseEntity<Void> {
        val attempt = loginAttemptService.createLoginAttempt(returnTo)
        val loginCookie = loginCookieService.createLoginCookie(attempt)
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(
                hubAuthenticationService.createAuthorizationUri(
                    attempt.state,
                    pkceService.createChallenge(attempt.codeVerifier),
                ),
            )
            .header(HttpHeaders.SET_COOKIE, loginCookie.toString())
            .cacheControl(CacheControl.noStore())
            .build()
    }

    @Operation(summary = "Complete Hub sign-in", description = "Creates a local session and redirects to the frontend.")
    @GetMapping(HUB_CALLBACK_PATH)
    fun callback(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @Schema(description = "Signed state returned unchanged by Hub")
        @RequestParam(required = false) state: String?,
        @Schema(description = "Single-use authorization code returned by Hub")
        @RequestParam(required = false) code: String?,
        @Schema(description = "OAuth error returned by Hub instead of an authorization code", example = "access_denied")
        @RequestParam(required = false) error: String?,
    ): ResponseEntity<Void> {
        val attempt = loginCookieService.validateAndConsumeLoginAttempt(request, response, state)
            ?: return errorResponse(HttpStatus.BAD_REQUEST)
        if (error != null) {
            log.warn("Rejected Hub callback: oauth_error")
            return errorResponse(HttpStatus.BAD_REQUEST)
        }
        if (code.isNullOrBlank()) {
            log.warn("Rejected Hub callback: missing_code")
            return errorResponse(HttpStatus.BAD_REQUEST)
        }
        val session = try {
            hubLoginService.login(code, attempt.codeVerifier)
        } catch (exception: HubAuthenticationException) {
            return errorResponse(
                when (exception.failure) {
                    HubAuthenticationFailure.OAUTH_ERROR -> HttpStatus.BAD_REQUEST
                    HubAuthenticationFailure.TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT
                    else -> HttpStatus.BAD_GATEWAY
                },
            )
        }
        val cookie = sessionCookieService.createSessionCookie(
            session.token,
            Duration.between(Instant.now(), session.expiresAt),
        )
        // Keep the login-cookie deletion alongside the new session cookie.
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(attempt.redirectUrl))
            .cacheControl(CacheControl.noStore())
            .build()
    }

    private fun errorResponse(status: HttpStatus): ResponseEntity<Void> = ResponseEntity.status(status)
        .cacheControl(CacheControl.noStore())
        .build()

    private companion object {
        val log = LoggerFactory.getLogger(HubAuthenticationController::class.java)
    }
}
