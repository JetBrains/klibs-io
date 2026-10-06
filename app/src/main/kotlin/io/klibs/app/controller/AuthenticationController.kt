package io.klibs.app.controller

import io.klibs.app.api.CurrentUserResponse
import io.klibs.app.auth.AuthenticatedUserPrincipal
import io.klibs.app.auth.SessionCookieService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
class AuthenticationController(
    private val sessionCookieService: SessionCookieService,
    private val userSessionService: UserSessionService,
) {
    @GetMapping("/current-user")
    fun currentUser(
        @AuthenticationPrincipal principal: AuthenticatedUserPrincipal?,
    ): CurrentUserResponse =
        CurrentUserResponse(
            authenticated = principal != null,
            userId = principal?.userId,
        )

    @PostMapping("/sign-out")
    fun signOut(request: HttpServletRequest): ResponseEntity<Void> {
        val token = sessionCookieService.getSessionToken(request)
            ?: return ResponseEntity.noContent()
                .build()

        userSessionService.deleteSession(token)
        val deletionCookie = sessionCookieService.createSessionCookieForDeletion()

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, deletionCookie.toString())
            .build()
    }
}
