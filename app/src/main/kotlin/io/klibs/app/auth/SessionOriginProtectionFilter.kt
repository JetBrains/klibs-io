package io.klibs.app.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

class SessionOriginProtectionFilter(
    private val sessionCookieService: SessionCookieService,
    private val trustedOriginValidator: TrustedOriginValidator,
    private val requestMatcher: RequestMatcher = AnyRequestMatcher.INSTANCE,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !requestMatcher.matches(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val sessionToken = sessionCookieService.getSessionToken(request)
        if (
            sessionToken != null &&
            request.method !in SAFE_HTTP_METHODS &&
            !trustedOriginValidator.isTrustedOrigin(request)
        ) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            return
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        val SAFE_HTTP_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
    }
}
