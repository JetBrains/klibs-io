package io.klibs.app.indexing

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class IndexRequestCheckService(
    private val gitHubIntegration: GitHubIntegration,
    private val requestIndexingService: RequestIndexingService,
    private val mavenCentralLogRepository: MavenCentralLogRepository,
    @Value("\${klibs.integration.github.index-requests.request-label}")
    private val requestLabel: String,
    @Value("\${klibs.integration.github.index-requests.processed-label}")
    private val processedLabel: String,
) {

    // TODO(Zofia Wiora): rename all index request to something with user_issue_request / github_issue_request
    // TODO(Zofia Wiora): Change zwiora/klibs in application-local before moving to PR? testing?

    // TODO(Zofia Wiora): comment
    fun checkIndexRequests() {
        val since = mavenCentralLogRepository.retrieveIndexRequestCheckTimestamp()
        val runStartedAt = Instant.now()

        // Get list of issues from GitHub
        val issues = try {
            gitHubIntegration.getKlibsIssuesByLabel(requestLabel, since)
        } catch (e: Exception) {
            logger.error("Failed to list index-request issues: ${e.message}", e)
            return
        }

        val issuesToProcess = issues.mapNotNull { issue ->
            // Check if issue used correct template
            val parsed = parseBody(issue.body)
            if (parsed == null) {
                publishIssueStatus(issue.number, IndexRequestMessages.parseFailure())
                return@mapNotNull null
            }

            // Check if data provided by user is valid
            val validationError = validateRequest(parsed)
            if (validationError != null) {
                publishIssueStatus(
                    issue.number,
                    IndexRequestMessages.failure(parsed.groupId, parsed.artifactId, parsed.version, validationError)
                )
                return@mapNotNull null
            }

            issue to parsed
        }
            .sortedBy { (_, parsed) -> parsed.version != null } // for duplicate check we place issues with `null` versions before issues with specific versions

        var anyServerError = false
        val processedRequests =
            mutableListOf<Pair<ParsedRequest, Int>>() // for duplicate check we track processed requests

        // Process issues
        for ((issue, parsed) in issuesToProcess) {
            try {
                if (!processValidRequest(issue, parsed, processedRequests)) anyServerError = true
            } catch (e: Exception) {
                logger.error("Unexpected error while processing issue #${issue.number}: ${e.message}", e)
                anyServerError = true
            }
        }

        // If any issue could not be processed because of errors that were not user's fault,
        // we don't update timestamp so that we can retry processing later
        if (!anyServerError) {
            mavenCentralLogRepository.saveIndexRequestCheckTimestamp(runStartedAt)
        } else {
            logger.warn("Server errors occurred; not updating index_request_check_timestamp")
        }
    }

    /**
     * @return true if the outcome is final (success or 4xx), false if server error occurred (5xx) — timestamp should not be updated
     */
    private fun processValidRequest(
        issue: GitHubIssue,
        parsed: ParsedRequest,
        processedRequests: MutableList<Pair<ParsedRequest, Int>>
    ): Boolean {

        val (groupId, artifactId, version) = parsed

        // Duplicate check
        val duplicateIssueNumber = findDuplicateIssueNumber(parsed, processedRequests)

        if (duplicateIssueNumber != null) {
            publishIssueStatus(
                issue.number,
                IndexRequestMessages.duplicate(groupId, artifactId, version, duplicateIssueNumber)
            )
            return true
        }

        // Try saving the package index request
        return try {
            requestIndexingService.requestIndexing(groupId, artifactId, version)
            publishIssueStatus(
                issue.number,
                IndexRequestMessages.success(groupId, artifactId, version)
            )

            // Add successfully processed requests to list for future duplicate check
            processedRequests.add(parsed to issue.number)
            true
        } catch (e: Exception) {
            // User's error occurred
            if (e is ResponseStatusException && e.statusCode.is4xxClientError) {
                publishIssueStatus(
                    issue.number,
                    IndexRequestMessages.failure(groupId, artifactId, version, e.reason ?: "Unknown error")
                )
                true
            }
            // Other error occurred
            else {
                logger.error("Server error on issue #${issue.number}: (${e.message})")
                false
            }
        }
    }

    internal data class ParsedRequest(val groupId: String, val artifactId: String, val version: String?)

    /**
     * @return null if the issue body is not a valid index-request
     */
    internal fun parseBody(body: String?): ParsedRequest? {
        if (body.isNullOrBlank()) return null

        val groupId = extractField(body, "Group ID")?.trim() ?: return null
        val artifactId = extractField(body, "Artifact ID")?.trim() ?: return null
        val version = extractField(body, "Version")?.trim()?.takeIf { it.isNotBlank() }

        return ParsedRequest(groupId, artifactId, version)
    }

    /**
     * @return null if the request is valid, or an error message if it is not
     */
    internal fun validateRequest(parsed: ParsedRequest): String? {
        // Regex for group id and artifact id: Alphanumeric characters, dots, underscores, and hyphens.
        val regex = "^[A-Za-z0-9_.-]+$".toRegex()

        if (!parsed.groupId.matches(regex)) {
            return "Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
        }
        if (!parsed.artifactId.matches(regex)) {
            return "Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
        }

        // We don't validate version format as it can include almost any characters

        return null
    }

    /**
     * Checks if the current request is a duplicate of an already processed request in the current batch.
     *
     * Issue is considered a duplicate if it has the same Group ID, Artifact ID,
     * and either Version is the same, or the previous request was for all versions (version == null)
     *
     * @return the issue number of the duplicate, or null if it is not a duplicate.
     */
    internal fun findDuplicateIssueNumber(
        currentRequest: ParsedRequest,
        processedRequests: List<Pair<ParsedRequest, Int>>
    ): Int? {
        return processedRequests.firstOrNull { (prevReq, _) ->
            prevReq.groupId == currentRequest.groupId &&
                    prevReq.artifactId == currentRequest.artifactId &&
                    (prevReq.version == null || prevReq.version == currentRequest.version)
        }?.second
    }

    private fun publishIssueStatus(issueNumber: Int, comment: String) {
        gitHubIntegration.addKlibsIssueComment(issueNumber, comment)
        gitHubIntegration.addKlibsIssueLabel(issueNumber, processedLabel)
    }

    private fun extractField(body: String, label: String): String? {
        // GitHub issue forms render fields as `### <Label>\n\n<value>\n\n### <next>`
        // In case user does not fill in the field, the value will be rendered as `_No response_`
        val regex = Regex("###\\s+${Regex.escape(label)}\\s*\\n+([\\s\\S]*?)(?=\\n###\\s|\\z)")
        val raw = regex.find(body)?.groupValues?.get(1)?.trim() ?: return null
        if (raw.isBlank() || raw.equals("_No response_", ignoreCase = true)) return null
        return raw
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRequestCheckService::class.java)
    }
}

private object IndexRequestMessages {
    fun success(groupId: String, artifactId: String, version: String?) = """
        ✅ Indexing request accepted

        Your request has been queued for indexing:

        - **Group ID:** `$groupId`
        - **Artifact ID:** `$artifactId`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`$version`"}

        The library will appear on https://klibs.io once indexing completes. A comment will be posted here with the final status once the processing is finished.
    """.trimIndent()

    fun failure(groupId: String, artifactId: String, version: String?, reason: String) = """
        ❌ Indexing request could not be processed

        - **Group ID:** `$groupId`
        - **Artifact ID:** `$artifactId`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`$version`"}

        **Reason:** $reason

        Please make sure the library meets the [indexing requirements](https://klibs.io/faq#how-do-i-add-a-project), then open a new issue with corrected details.

        Once you have reviewed this and are ready to open a new request, please close this issue.
    """.trimIndent()

    fun duplicate(groupId: String, artifactId: String, version: String?, duplicateOfIssueNumber: Int) = """
        ❌ Indexing request could not be processed

        - **Group ID:** `$groupId`
        - **Artifact ID:** `$artifactId`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`$version`"}

        **Reason:** This request is a duplicate of #$duplicateOfIssueNumber.
        ${if (!version.isNullOrBlank()) " Note, that if the other request was for all versions, this one is considered a duplicate as well." else ""} 

        Please follow the existing request instead of opening another one.

        Once you have reviewed this, please close this issue.
    """.trimIndent()

    fun parseFailure() = """
        ❌ Could not read the indexing request

        We couldn't parse the input fields from this issue. Please open a new issue using the *"Request indexing of a library"* template and fill in all required fields.

        Once you have reviewed this and are ready to open a new request, please close this issue.
    """.trimIndent()
}