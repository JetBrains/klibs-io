package io.klibs.app.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import java.net.URI

class TrustedOriginValidator(
    private val trustedOrigin: String,
    requireHttps: Boolean = false,
) {
    init {
        val uri = URI.create(trustedOrigin)
        require(uri.scheme == "http" || uri.scheme == "https") {
            "Trusted frontend origin must use HTTP or HTTPS"
        }
        require(!requireHttps || uri.scheme == "https") {
            "Trusted frontend origin must use HTTPS"
        }
        require(!uri.host.isNullOrBlank()) {
            "Trusted frontend origin must contain a host"
        }
        require(uri.userInfo == null && uri.path.isNullOrEmpty() && uri.query == null && uri.fragment == null) {
            "Trusted frontend origin must contain only scheme, host, and optional port"
        }
    }

    fun isTrustedOrigin(request: HttpServletRequest): Boolean =
        request.getHeader(HttpHeaders.ORIGIN) == trustedOrigin
}
