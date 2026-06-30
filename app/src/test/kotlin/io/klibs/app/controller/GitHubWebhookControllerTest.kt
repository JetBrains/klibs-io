package io.klibs.app.controller

import SmokeTestBase
import io.klibs.app.service.UserIssueNotifier
import io.klibs.app.service.impl.DefaultUserRequestService
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.post
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@TestPropertySource(
    properties = [
        "klibs.integration.github.webhook.secret=$WEBHOOK_SECRET",
    ]
)
class GitHubWebhookControllerTest : SmokeTestBase() {

    @MockitoBean
    private lateinit var userRequestService: DefaultUserRequestService

    @MockitoBean
    private lateinit var userIssueNotifier: UserIssueNotifier

    @Test
    fun `accepts a valid issues opened delivery and dispatches to the service`() {
        val payload = openedPayload(
            number = 42,
            labels = listOf("index-request"),
            body = "### Group ID\n\norg.example\n\n### Artifact ID\n\nlib\n\n"
        )

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-GitHub-Delivery", "test-1")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isAccepted() }
        }

        verify(userRequestService).processRequest(argThat<UserIndexingRequestDto> {
            githubIssueNumber == 42 && groupId == "org.example" && artifactId == "lib"
        })
    }

    @Test
    fun `rejects an invalid signature with 401 and does not invoke the service`() {
        val payload = openedPayload(number = 1, labels = listOf("index-request"), body = "x")

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-Hub-Signature-256", "sha256=deadbeef")
        }.andExpect {
            status { isUnauthorized() }
        }

        verify(userRequestService, never()).processRequest(any())
    }

    @Test
    fun `responds 200 to a ping event`() {
        val payload = "{}".toByteArray()

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "ping")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isOk() }
        }

        verify(userRequestService, never()).processRequest(any())
    }

    @Test
    fun `ignores actions other than opened`() {
        val payload = """
            {
              "action": "closed",
              "issue": { "number": 7, "body": "x", "labels": [ { "name": "index-request" } ], "created_at": "2025-01-01T00:00:00Z" }
            }
        """.trimIndent().toByteArray()

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isNoContent() }
        }

        verify(userRequestService, never()).processRequest(any())
    }

    @Test
    fun `ignores issues without the request label`() {
        val payload = openedPayload(number = 8, labels = listOf("bug"), body = "x")

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isNoContent() }
        }

        verify(userRequestService, never()).processRequest(any())
    }

    @Test
    fun `responds 400 on malformed json`() {
        val payload = "not-json".toByteArray()

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isBadRequest() }
        }

        verify(userRequestService, never()).processRequest(any())
    }

    @Test
    fun `responds 400 on parse failure and notifies user`() {
        val payload = openedPayload(
            number = 99,
            labels = listOf("index-request"),
            body = "invalid body"
        )

        mockMvc.post("/webhooks/github/issues") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
            header("X-GitHub-Event", "issues")
            header("X-GitHub-Delivery", "test-parse-failure")
            header("X-Hub-Signature-256", sign(payload))
        }.andExpect {
            status { isBadRequest() }
        }

        verify(userRequestService, never()).processRequest(any())
        verify(userIssueNotifier).notifyFailure(99, null)
    }

    private fun openedPayload(number: Int, labels: List<String>, body: String): ByteArray {
        val labelsJson = labels.joinToString(prefix = "[", postfix = "]") { """{"name":"$it"}""" }
        val escapedBody = body.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """
            {
              "action": "opened",
              "issue": {
                "number": $number,
                "body": "$escapedBody",
                "labels": $labelsJson,
                "created_at": "2025-01-01T00:00:00Z"
              }
            }
        """.trimIndent().toByteArray()
    }

    private fun sign(payload: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(WEBHOOK_SECRET.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }

}

private const val WEBHOOK_SECRET = "test-secret"
