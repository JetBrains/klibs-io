package io.klibs.core.user.service

import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties.Companion.ENABLED_PROPERTY
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE S256 binds the authorization code to the secret generated before redirecting the browser to Hub. */
@Service
@ConditionalOnProperty(ENABLED_PROPERTY, havingValue = "true")
class PkceService(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun createVerifier(): String {
        val bytes = ByteArray(VERIFIER_SIZE_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun createChallenge(codeVerifier: String): String {
        // Hub receives this challenge first, then hashes the verifier during token exchange to verify the match.
        val digest = MessageDigest.getInstance(CHALLENGE_DIGEST).digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private companion object {
        const val VERIFIER_SIZE_BYTES = 32
        const val CHALLENGE_DIGEST = "SHA-256"
    }
}
