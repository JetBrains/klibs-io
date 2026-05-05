package io.klibs.app.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value
        if (token != null) {
            val login = jwtService.extractLogin(token)
            if (login != null && SecurityContextHolder.getContext().authentication == null) {
                log.debug("Authenticated request from GitHub user: {}", login)
                val auth = UsernamePasswordAuthenticationToken(
                    login, null, listOf(SimpleGrantedAuthority("ROLE_AUTHOR"))
                )
                SecurityContextHolder.getContext().authentication = auth
            } else if (login == null) {
                log.debug("Invalid or expired JWT cookie, ignoring")
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val COOKIE_NAME = "klibs_session"
        private val log = LoggerFactory.getLogger(JwtAuthFilter::class.java)
    }
}
