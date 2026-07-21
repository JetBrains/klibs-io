package io.klibs.app.service.impl

import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import io.klibs.integration.github.service.impl.DefaultGitHubWebhookSignatureVerifier
import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubWebhookSignatureVerifierTest {

    private val secret = "It's a Secret to Everybody"
    private val verifier = newVerifier(secret)

    @Test
    fun `accepts a correctly signed payload`() {
        val payload = """{"hello":"world"}""".toByteArray()
        val sig = "sha256=" + hmacSha256Hex(secret, payload)

        assertTrue(verifier.isValid(payload, sig))
    }

    @Test
    fun `rejects a payload signed with a different secret`() {
        val payload = "tampered".toByteArray()
        val sig = "sha256=" + hmacSha256Hex("wrong-secret", payload)

        assertFalse(verifier.isValid(payload, sig))
    }

    @Test
    fun `rejects a missing or blank signature header`() {
        val payload = "anything".toByteArray()
        assertFalse(verifier.isValid(payload, null))
        assertFalse(verifier.isValid(payload, ""))
        assertFalse(verifier.isValid(payload, "   "))
    }

    @Test
    fun `rejects when the signature header has the wrong prefix`() {
        val payload = "anything".toByteArray()
        val withoutPrefix = hmacSha256Hex(secret, payload)
        assertFalse(verifier.isValid(payload, withoutPrefix))
        assertFalse(verifier.isValid(payload, "sha1=$withoutPrefix"))
    }

    @Test
    fun `rejects everything when no secret is configured`() {
        val empty = newVerifier("")
        val payload = "anything".toByteArray()
        val sig = "sha256=" + hmacSha256Hex("some-secret", payload)
        assertFalse(empty.isValid(payload, sig))
    }

    private fun newVerifier(secret: String): DefaultGitHubWebhookSignatureVerifier {
        val properties = GitHubIntegrationProperties(
            webhook = GitHubIntegrationProperties.Webhook(secret = secret),
            cache = GitHubIntegrationProperties.Cache(),
            indexRequests = GitHubIntegrationProperties.IndexRequests()
        )
        return DefaultGitHubWebhookSignatureVerifier(properties)
    }

    private fun hmacSha256Hex(key: String, payload: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }
}
