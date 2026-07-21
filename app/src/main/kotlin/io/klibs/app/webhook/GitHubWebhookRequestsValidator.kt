package io.klibs.app.webhook

import io.klibs.app.dto.UserIndexingRequestValidationResult
import io.klibs.app.api.GitHubWebhookUserIndexingRequest
import io.klibs.app.mapper.GitHubWebhookMapper
import io.klibs.app.service.UserIssueNotifier
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/**
 * Validates the applicability of a GitHub webhook request.
 */
@Component
class GitHubWebhookRequestsValidator(
    private val gitHubWebhookMapper: GitHubWebhookMapper,
    private val userIssueNotifier: UserIssueNotifier,
    private val gitHubIntegrationProperties: GitHubIntegrationProperties
) {

    private val logger = LoggerFactory.getLogger(GitHubWebhookRequestsValidator::class.java)

    fun validateUserIndexingRequest(
        payload: GitHubWebhookUserIndexingRequest,
        event: String?,
        delivery: String?
    ): UserIndexingRequestValidationResult {
        if (event == "ping") {
            logger.info("Received GitHub webhook ping (delivery=$delivery)")
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.ok().build())
        }

        if (event != "issues") {
            logger.debug("Ignoring GitHub event '$event' (delivery=$delivery)")
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.noContent().build())
        }

        if (payload.action != "opened") {
            logger.debug("Ignoring issues action '${payload.action}' (delivery=$delivery)")
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.noContent().build())
        }

        val issuePayload = payload.issue ?: run {
            logger.warn("Missing `issue` object in webhook delivery $delivery")
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.badRequest().build())
        }

        val requestLabel = gitHubIntegrationProperties.indexRequests.requestLabel
        if (issuePayload.labels.none { it.name == requestLabel }) {
            logger.debug(
                "Ignoring issue #${issuePayload.number} (delivery=$delivery): missing required label '$requestLabel'"
            )
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.noContent().build())
        }

        val issueNumber = issuePayload.number
        val issueDto = gitHubWebhookMapper.toUserRequestIssueDto(issuePayload) ?: run {
            logger.warn("Could not map `issue` object from webhook delivery $delivery")
            if (issueNumber != null) {
                userIssueNotifier.notifyFailure(issueNumber, null)
            }
            return UserIndexingRequestValidationResult.NotApplicable(ResponseEntity.badRequest().build())
        }

        return UserIndexingRequestValidationResult.Valid(issueDto)
    }
}

