package io.klibs.app.controller

import io.klibs.app.api.GitHubWebhookUserIndexingRequest
import io.klibs.app.service.UserRequestService
import io.klibs.app.dto.UserIndexingRequestValidationResult
import io.klibs.app.webhook.GitHubWebhookRequestsValidator
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Receives GitHub `issues` webhook deliveries for the klibs.io repository and
 * dispatches them for asynchronous processing.
 *
 * Verifies the `X-Hub-Signature-256` header on every request and only acts on
 * `issues.opened` events whose issue carries the configured request label.
 * The HTTP response is returned immediately (202 Accepted), the actual indexing
 * work happens asynchronously within the service layer. Server-side errors during
 * processing are not exposed back to
 * GitHub — they're logged, and GitHub's built-in redelivery is not used; the
 * webhook is fire-and-forget once accepted.
 */
@RestController
@RequestMapping("/webhooks/github")
class GitHubWebhookController(
    private val userRequestService: UserRequestService,
    private val gitHubWebhookRequestsValidator: GitHubWebhookRequestsValidator,
) {

    @PostMapping("/issues", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun handleIssues(
        @RequestBody payload: GitHubWebhookUserIndexingRequest,
        @RequestHeader(name = "X-GitHub-Event", required = false) event: String?,
        @RequestHeader(name = "X-GitHub-Delivery", required = false) delivery: String?,
    ): ResponseEntity<Void> {
        val requestValidationResult = gitHubWebhookRequestsValidator.validateUserIndexingRequest(payload, event, delivery)
        if (!requestValidationResult.isValidRequest()) return (requestValidationResult as UserIndexingRequestValidationResult.NotApplicable).response

        userRequestService.processRequest((requestValidationResult as UserIndexingRequestValidationResult.Valid).request)

        return ResponseEntity.accepted().build()
    }
}
