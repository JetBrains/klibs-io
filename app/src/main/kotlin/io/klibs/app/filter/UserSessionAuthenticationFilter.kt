package io.klibs.app.filter

import io.klibs.app.auth.AUTHENTICATED_USER_AUTHORITY
import io.klibs.app.auth.AuthenticatedUserPrincipal
import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

class UserSessionAuthenticationFilter(
    private val sessionCookieService: SessionCookieService,
    private val userSessionService: UserSessionService,
    private val requestMatcher: RequestMatcher = AnyRequestMatcher.INSTANCE,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !requestMatcher.matches(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val sessionToken = sessionCookieService.getSessionToken(request)
        if (sessionToken == null) {
            filterChain.doFilter(request, response)
            return
        }

        val session = userSessionService.authenticateAndRefreshSessionIfAlive(sessionToken)
        if (session == null) {
            log.debug(
                "Discarding invalid or expired session cookie: method={}, path={}",
                request.method,
                request.requestURI,
            )
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                sessionCookieService.createSessionCookieForDeletion().toString(),
            )
            filterChain.doFilter(request, response)
            return
        }

        val authentication = PreAuthenticatedAuthenticationToken(
            AuthenticatedUserPrincipal(session.userId),
            null,
            listOf(SimpleGrantedAuthority(AUTHENTICATED_USER_AUTHORITY)),
        )
        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)

        session.refreshedCookieTtl?.let { cookieTtl ->
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                sessionCookieService.createSessionCookie(sessionToken, cookieTtl).toString(),
            )
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(UserSessionAuthenticationFilter::class.java)
    }
}
