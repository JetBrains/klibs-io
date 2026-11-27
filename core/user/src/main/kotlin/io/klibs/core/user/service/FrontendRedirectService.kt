package io.klibs.core.user.service

import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties.Companion.ENABLED_PROPERTY
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLDecoder

@Service
@ConditionalOnProperty(ENABLED_PROPERTY, havingValue = "true")
class FrontendRedirectService {
    fun sanitizeReturnTo(returnTo: String?): String {
        if (returnTo == null) return FRONTEND_ROOT
        return parseSafeReturnTo(returnTo) ?: rejectedReturnTo()
    }

    private fun parseSafeReturnTo(returnTo: String): String? {
        return try {
            val parsed = URI.create(returnTo)
            // Decode the URL only to check it; the original encoding is kept so query values like C%2B%2B do not change.
            val decoded = URLDecoder.decode(returnTo, Charsets.UTF_8)
            if (!isSafeRelativePath(returnTo, decoded)) return null
            // Limit the path length so it fits in the login cookie along with the other login data.
            parsed.toASCIIString().takeIf { it.length <= MAX_RETURN_TO_LENGTH }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun isSafeRelativePath(returnTo: String, decoded: String): Boolean =
        returnTo.startsWith(FRONTEND_ROOT) && !decoded.startsWith("//") &&
            decoded.none { it == '\\' || it.isISOControl() }

    private fun rejectedReturnTo(): String {
        log.warn("Rejected unsafe OAuth returnTo; using the frontend root")
        return FRONTEND_ROOT
    }

    private companion object {
        val log = LoggerFactory.getLogger(FrontendRedirectService::class.java)
        const val FRONTEND_ROOT = "/"
        const val MAX_RETURN_TO_LENGTH = 1024
    }
}
