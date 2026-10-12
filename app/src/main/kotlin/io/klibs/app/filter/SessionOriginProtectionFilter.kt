package io.klibs.app.filter

import io.klibs.app.auth.TrustedOriginValidator
import io.klibs.app.service.SessionCookieService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
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
            log.warn(
                "Rejected request carrying a session cookie due to untrusted origin: method={}, path={}, origin={}",
                request.method,
                request.requestURI,
                request.getHeader(HttpHeaders.ORIGIN),
            )
            response.status = HttpServletResponse.SC_FORBIDDEN
            return
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SessionOriginProtectionFilter::class.java)

        val SAFE_HTTP_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
    }
}
