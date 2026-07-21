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
 * Receives GitHub `issues` webhook deliveries and processes them.
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
        return when (val result = gitHubWebhookRequestsValidator.validateUserIndexingRequest(payload, event, delivery)) {
            is UserIndexingRequestValidationResult.NotApplicable -> result.response
            is UserIndexingRequestValidationResult.Valid -> {
                userRequestService.processRequest(result.request)
                ResponseEntity.accepted().build()
            }
        }
    }
}
