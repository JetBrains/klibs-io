package io.klibs.integration.github.service.impl

import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import io.klibs.integration.github.service.GitHubWebhookSignatureVerifier
import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies the `X-Hub-Signature-256` header of incoming GitHub webhook deliveries.
 *
 * GitHub signs the raw request body with HMAC SHA-256 using the shared secret
 * configured for the webhook and sends the resulting hex digest in the
 * `X-Hub-Signature-256` header in the form `sha256=<hex>`.
 */
@Component
class DefaultGitHubWebhookSignatureVerifier(
    private val properties: GitHubIntegrationProperties,
) : GitHubWebhookSignatureVerifier {

    companion object {
        private const val SIGNATURE_PREFIX = "sha256="
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    /**
     * Returns `true` if [signatureHeader] matches the HMAC SHA-256 of [payload]
     * computed with the configured secret.
     */
    override fun isValid(payload: ByteArray, signatureHeader: String?): Boolean {
        val secret = properties.webhook.secret
        if (secret.isNullOrBlank()) return false
        if (signatureHeader.isNullOrBlank()) return false
        if (!signatureHeader.startsWith(SIGNATURE_PREFIX)) return false

        val expected = SIGNATURE_PREFIX + computeSignature(payload, secret)

        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            signatureHeader.toByteArray(Charsets.UTF_8)
        )
    }

    private fun computeSignature(payload: ByteArray, secret: String): String {
        val messageAuthenticationCode = Mac.getInstance(HMAC_ALGORITHM)
        messageAuthenticationCode.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        val digest = messageAuthenticationCode.doFinal(payload)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}