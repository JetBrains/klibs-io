package io.klibs.app.filter

import io.klibs.app.configuration.GITHUB_WEBHOOK_PATH
import io.klibs.integration.github.service.GitHubWebhookSignatureVerifier
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that validates the `X-Hub-Signature-256` header for GitHub webhook requests.
 */
class GitHubWebhookSignatureFilter(
    private val signatureVerifier: GitHubWebhookSignatureVerifier
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith(GITHUB_WEBHOOK_PATH)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = CachedBodyHttpServletRequest(request)
        val signature = wrappedRequest.getHeader("X-Hub-Signature-256")
        val body = wrappedRequest.getCachedBody()

        if (signatureVerifier.isValid(body, signature)) {
            filterChain.doFilter(wrappedRequest, response)
        } else {
            log.warn("Rejected GitHub webhook delivery: invalid or missing signature (URI: ${request.requestURI})")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing GitHub signature")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GitHubWebhookSignatureFilter::class.java)
    }
}
