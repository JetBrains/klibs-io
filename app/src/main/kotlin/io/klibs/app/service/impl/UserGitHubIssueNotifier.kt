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

    override fun notifyAccepted(issueNumber: Int) {
        publishIssueStatus(issueNumber, GitHubUserRequestMessages.accepted())
        logger.debug("Published success comment to issue #$issueNumber")
    }

    override fun notifyFailure(issueNumber: Int, reason: String?) {
        publishIssueStatus(
            issueNumber,
            GitHubUserRequestMessages.failure(reason, gitHubIntegrationProperties.indexRequests.developerHandle)
        )
        logger.debug("Published failure comment to issue #$issueNumber: ${reason ?: "Unknown error"}")
    }

    override fun notifyServerErrorFailure(issueNumber: Int) {
        publishIssueStatus(
            issueNumber,
            GitHubUserRequestMessages.serverErrorNotification(gitHubIntegrationProperties.indexRequests.developerHandle)
        )
        logger.debug("Published server error failure comment to issue #$issueNumber")
    }

    override fun notifyIndexingSuccess(issueNumber: Int) {
        gitHubIntegration.addKlibsIssueComment(
            issueNumber,
            GitHubUserRequestMessages.indexingSucceeded(gitHubIntegrationProperties.indexRequests.developerHandle)
        )
        logger.debug("Published indexing-succeeded comment to issue #$issueNumber")
    }

    override fun notifyIndexingFailure(
        issueNumber: Int,
        reason: String?
    ) {
        gitHubIntegration.addKlibsIssueComment(
            issueNumber,
            GitHubUserRequestMessages.indexingFailed(
                reason,
                gitHubIntegrationProperties.indexRequests.developerHandle
            )
        )
        logger.debug("Published indexing-failed comment to issue #$issueNumber")
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

    fun accepted() = """
        ⏳Indexing request accepted

        The package will appear on https://klibs.io once indexing completes. A comment will be posted here with the final status once the processing is finished.
    """.trimIndent()

    fun failure(reason: String?, developer: String?) = """
        ❌ Indexing request could not be processed
        
        ${reason?.let { "**Reason:** $it" } ?: ""}

        Please make sure the package meets the [indexing requirements](https://klibs.io/faq#how-do-i-add-a-project), then open a new issue with corrected details.

        ${developer?.let { "cc @$it" } ?: ""}
    """.trimIndent()

    fun serverErrorNotification(developer: String?) = """
        ❌ Indexing request could not be processed
        
        Sorry, some unexpected server error happened during processing your request.
        
        Developers have been notified about this internal error.
        
        ${developer?.let { "cc @$it" } ?: ""}
        
    """.trimIndent()

    fun indexingSucceeded(developer: String?) = """
        ✅ Indexing completed

        The requested package has been indexed and should now be available on https://klibs.io.

        You can now close this issue.

        ${developer?.let { "cc @$it" } ?: ""}
    """.trimIndent()

    fun indexingFailed(reason: String?, developer: String?) = """
        ❌ Indexing failed

        We accepted your request, but the package could not be indexed.

        ${reason?.let { "**Reason:** $it" } ?: ""}

        ${developer?.let { "cc @$it" } ?: ""}
    """.trimIndent()
}
