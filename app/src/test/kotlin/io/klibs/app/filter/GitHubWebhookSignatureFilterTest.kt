package io.klibs.app.filter

import SmokeTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.post
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@TestPropertySource(
    properties = [
        "klibs.integration.github.webhook.secret=$WEBHOOK_SECRET",
    ]
)
class GitHubWebhookSignatureFilterTest : SmokeTestBase() {

    @Test
    fun `rejects missing signature`() {
        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `rejects invalid signature`() {
        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
            header("X-Hub-Signature-256", "sha256=invalid")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `rejects signature with wrong prefix`() {
        val payload = "{}".toByteArray()
        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-Hub-Signature-256", sign(payload).replace("sha256=", "sha1="))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `accepts valid signature and controller can read body`() {
        val payload = "{}".toByteArray()
        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "ping")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isOk() }
        }
    }

    private fun sign(payload: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(WEBHOOK_SECRET.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }
}

private const val WEBHOOK_SECRET = "test-secret"
