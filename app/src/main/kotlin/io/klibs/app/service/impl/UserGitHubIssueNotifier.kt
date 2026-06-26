package io.klibs.app.service.impl

import io.klibs.app.service.UserIssueNotifier
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * GitHub-specific implementation of [UserIssueNotifier].
 */
@Service
internal class UserGitHubIssueNotifier(
    private val gitHubIntegration: GitHubIntegration,
    private val gitHubIntegrationProperties: GitHubIntegrationProperties
) : UserIssueNotifier {

    override fun notifySuccess(issueNumber: Int) {
        publishIssueStatus(issueNumber, GitHubUserRequestMessages.success())
        logger.debug("Published success comment to issue #$issueNumber")
    }

    override fun notifyFailure(issueNumber: Int, reason: String?) {
        publishIssueStatus(issueNumber, GitHubUserRequestMessages.failure(reason))
        logger.debug("Published failure comment to issue #$issueNumber: ${reason ?: "Unknown error"}")
    }

    override fun notifyParseFailure(issueNumber: Int) {
        publishIssueStatus(issueNumber, GitHubUserRequestMessages.parseFailure())
        logger.debug("Published parse failure comment to issue #$issueNumber")
    }

    override fun notifyServerErrorFailure(issueNumber: Int) {
        publishIssueStatus(
            issueNumber,
            GitHubUserRequestMessages.serverErrorNotification(gitHubIntegrationProperties.indexRequests.developerHandle)
        )
        logger.debug("Published server error failure comment to issue #$issueNumber")
    }

    private fun publishIssueStatus(issueNumber: Int, comment: String) {
        gitHubIntegration.addKlibsIssueComment(issueNumber, comment)
        gitHubIntegration.addKlibsIssueLabel(issueNumber, gitHubIntegrationProperties.indexRequests.processedLabel)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserGitHubIssueNotifier::class.java)
    }
}

/**
 * Messages displayed to users when their indexing request is processed.
 */
private object GitHubUserRequestMessages {

    fun success() = """
        ✅ Indexing request accepted

        The library will appear on https://klibs.io once indexing completes. A comment will be posted here with the final status once the processing is finished.
    """.trimIndent()

    fun failure(reason: String?) = """
        ❌ Indexing request could not be processed
        
        ${reason?.let { "**Reason:** $it" }}

        Please make sure the library meets the [indexing requirements](https://klibs.io/faq#how-do-i-add-a-project), then open a new issue with corrected details.

        Once you have reviewed this comment and are ready to open a new request, please close this issue.
    """.trimIndent()

    fun parseFailure() = """
        ❌ Could not read the indexing request

        We couldn't parse the input fields from this issue. Please open a new issue using the *"Request indexing of a library"* template and fill in all required fields.

        Once you have reviewed this comment and are ready to open a new request, please close this issue.
    """.trimIndent()

    fun serverErrorNotification(developer: String?) = """
        ❌ Indexing request could not be processed
        
        Sorry, some unexpected server error happened during processing your request.
        
        Developers have been notified about this internal error.
        
        ${developer.let { "cc @$it" }}
        
    """.trimIndent()
}
