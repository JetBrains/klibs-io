package io.klibs.app.indexing

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubUserRequestIssue
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * Service responsible for periodically checking GitHub for new user-submitted
 * package indexing requests and processing them.
 *
 * @property gitHubIntegration Client to interact with the GitHub API.
 * @property userRequestIndexingService Service to execute the actual package indexing.
 * @property mavenCentralLogRepository Repository to track the last check timestamp.
 * @property requestLabel The label used to identify issues that contain indexing requests.
 * @property processedLabel The label applied to issues after they have been processed.
 */
@Service
class UserRequestCheckService(
    private val gitHubIntegration: GitHubIntegration,
    private val userRequestIndexingService: UserRequestIndexingService,
    private val mavenCentralLogRepository: MavenCentralLogRepository,
    @Value("\${klibs.integration.github.index-requests.request-label}")
    private val requestLabel: String,
    @Value("\${klibs.integration.github.index-requests.processed-label}")
    private val processedLabel: String,
) {

    /**
     * Entry point for the scheduled job.
     *
     * Retrieves a batch of new GitHub issues since the last recorded check timestamp.
     * Iterates through the issues, validating and processing them individually.
     * Updates the timestamp in the database only if all issues were processed
     * without server-side errors.
     */
    fun checkUserRequests() {
        val since = mavenCentralLogRepository.retrieveUserRequestCheckTimestamp()
        val runStartedAt = Instant.now()

        // Get batch of issues from GitHub
        val issuesBatch = try {
            gitHubIntegration.getKlibsIssuesByLabel(requestLabel, since)
        } catch (e: Exception) {
            logger.error("Failed to list index-request issues: ${e.message}", e)
            return
        }

        val issuesToProcess = issuesBatch.issues.mapNotNull { issue ->
            // Check if issue used correct template
            val parsed = parseBody(issue.body)
            if (parsed == null) {
                publishIssueStatus(issue.number, UserRequestMessages.parseFailure())
                logger.debug("Published parse failure comment to issue #${issue.number}")
                return@mapNotNull null
            }

            // Check if data provided by user is valid
            val validationError = validateRequest(parsed)
            if (validationError != null) {
                publishIssueStatus(
                    issue.number,
                    UserRequestMessages.failure(parsed.groupId, parsed.artifactId, parsed.version, validationError)
                )
                logger.debug("Published failure comment to issue #${issue.number}: $validationError")
                return@mapNotNull null
            }

            issue to parsed
        }

        var anyServerError = false // tracked for timestamp update
        val processedRequests = mutableListOf<ProcessedRequestInfo>()  // tracked for duplicate check

        // Process issues
        for ((issue, parsed) in issuesToProcess) {
            try {
                if (!processValidRequest(issue, parsed, processedRequests)) anyServerError = true
            } catch (e: Exception) {
                logger.error("Unexpected error while processing issue #${issue.number}: ${e.message}", e)
                anyServerError = true
            }
        }

        // Update timestamp
        if (!anyServerError) {
            if (issuesBatch.hasMore == true) {
                // If not all requests were processed, next time only issues newer than the last processed will be fetched
                mavenCentralLogRepository.saveUserRequestCheckTimestamp(issuesBatch.issues.last().createdAt)
                logger.debug(
                    "Successfully processed all users' indexing requests fetched; updating user_request_check_timestamp to last issue's created_at: {}",
                    issuesBatch.issues.last().createdAt
                )
            } else {
                // If all requests were processed, next time only newly appeared issues will be fetched
                mavenCentralLogRepository.saveUserRequestCheckTimestamp(runStartedAt)
                if (issuesBatch.issues.isEmpty()) {
                    logger.debug(
                        "No users' indexing requests to be processed; updating user_request_check_timestamp to current date: {}",
                        runStartedAt
                    )
                } else {
                    logger.debug(
                        "Successfully processed all users' indexing requests; updating user_request_check_timestamp to current date: {}",
                        runStartedAt
                    )
                }
            }
        } else {
            // If any issue could not be processed because of errors that were not user's fault,
            // timestamp is not updated, so that failed issues can be reprocessed
            logger.warn("Server errors occurred; not updating index_request_check_timestamp")
        }
    }

    /**
     * Processes a single user request.
     *
     * Returns true if the outcome is final (success or 4xx), false if server error occurred (5xx) — timestamp should not be updated
     */
    private fun processValidRequest(
        issue: GitHubUserRequestIssue,
        parsed: ParsedRequest,
        processedRequests: MutableList<ProcessedRequestInfo>
    ): Boolean {
        val (groupId, artifactId, version) = parsed

        // Duplicate check
        val duplicateIssueNumber = findDuplicateIssueNumber(parsed, processedRequests)

        if (duplicateIssueNumber != null) {
            publishIssueStatus(
                issue.number,
                UserRequestMessages.duplicate(groupId, artifactId, version, duplicateIssueNumber)
            )
            logger.debug("Published comment to issue #${issue.number}: duplicate of #$duplicateIssueNumber")
            return true
        } else {
            processedRequests.add(ProcessedRequestInfo(parsed, issue.number))
        }

        // Try saving the package index request
        return try {
            userRequestIndexingService.indexUserRequest(groupId, artifactId, version)
            publishIssueStatus(
                issue.number,
                UserRequestMessages.success(groupId, artifactId, version)
            )

            logger.debug("Published success comment to issue #${issue.number}")
            true
        } catch (e: Exception) {
            // User's error occurred
            if (e is ResponseStatusException && e.statusCode.is4xxClientError) {
                publishIssueStatus(
                    issue.number,
                    UserRequestMessages.failure(groupId, artifactId, version, e.reason ?: "Unknown error")
                )

                logger.debug("Published failure comment to issue #${issue.number}: ${e.reason ?: "Unknown error"}")
                true
            }
            // Other error occurred
            else {
                logger.error("Server error on issue #${issue.number}: (${e.message})")
                false
            }
        }
    }

    internal data class ParsedRequest(
        val groupId: String,
        val artifactId: String,
        val version: String?
    )

    internal data class ProcessedRequestInfo(
        val request: ParsedRequest,
        val issueNumber: Int
    )

    /**
     * Extracts groupId, artifactId, and version from the Markdown issue body.
     *
     * Returns null if the expected values cannot be extracted from the issue body
     */
    internal fun parseBody(body: String?): ParsedRequest? {
        if (body.isNullOrBlank()) return null

        val groupId = extractField(body, "Group ID") ?: return null
        val artifactId = extractField(body, "Artifact ID") ?: return null
        val version = extractField(body, "Version")

        return ParsedRequest(groupId, artifactId, version)
    }

    /**
     * Checks if the submitted data is in valid format.
     *
     * Returns null if the request is valid, or an error message if it is not
     */
    internal fun validateRequest(parsed: ParsedRequest): String? {
        // Regex for group id and artifact id: Alphanumeric characters, dots, underscores, and hyphens.
        val regex = "^[A-Za-z0-9_.-]+$".toRegex()

        // Regex for version: Forbidding control characters, and characters manipulating URL path
        val versionRegex = "^[^\\p{Cntrl}/\\\\%?#&]+$".toRegex()

        if (!parsed.groupId.matches(regex)) {
            return "Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
        }
        if (!parsed.artifactId.matches(regex)) {
            return "Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
        }
        if (parsed.version != null && !parsed.version.matches(versionRegex)) {
            return "Invalid Version format. Newlines, other control characters, and the following characters are not allowed: /, \\, %, ?, #, &."
        }

        return null
    }

    /**
     * Checks if the current request is a duplicate of an already processed request in the current batch.
     *
     * Returns the issue number of the duplicate, or null if it is not a duplicate.
     */
    internal fun findDuplicateIssueNumber(
        currentRequest: ParsedRequest,
        processedRequests: List<ProcessedRequestInfo>
    ): Int? {
        return processedRequests.firstOrNull { prev ->
            val prevReq = prev.request

            prevReq.groupId == currentRequest.groupId &&
                    prevReq.artifactId == currentRequest.artifactId &&
                    prevReq.version == currentRequest.version

        }?.issueNumber
    }

    private fun publishIssueStatus(issueNumber: Int, comment: String) {
        gitHubIntegration.addKlibsIssueComment(issueNumber, comment)
        gitHubIntegration.addKlibsIssueLabel(issueNumber, processedLabel)
    }

    private fun extractField(body: String, label: String): String? {
        // GitHub issue forms render fields as `### <Label>\n\n<value>\n\n### <next>`.
        // In case user does not fill in the field, the value will be rendered as `_No response_`.
        // We don't use regex here to avoid potential performance issues in case of large input
        // (it shouldn't be a case for valid requests, but can happen in case of malicious intent)

        val text = body.replace("\r\n", "\n")

        val header = "### $label\n"

        if (!text.contains(header)) return null

        val raw = text
            .substringAfter(header)
            .substringBefore("\n### ")
            .trim()

        if (raw.isBlank() || raw.equals("_No response_", ignoreCase = true)) return null
        return raw
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserRequestCheckService::class.java)
    }
}

/**
 * Messages displayed to users when their indexing request is processed.
 */
private object UserRequestMessages {
    /**
     * Sanitizes raw user input for safe display inside GitHub inline code blocks.
     * Removes newlines, normalizes whitespace, escapes backticks, and truncates long strings.
     */
    private fun sanitize(input: String): String {
        val singleLine = input.replace(Regex("\\s+"), " ").replace("`", "'").trim()
        return if (singleLine.length > 50) "${singleLine.take(47)}..." else singleLine
    }

    fun success(groupId: String, artifactId: String, version: String?) = """
        ✅ Indexing request accepted

        Your request has been queued for indexing:

        - **Group ID:** `${sanitize(groupId)}`
        - **Artifact ID:** `${sanitize(artifactId)}`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`${sanitize(version)}`"}
        
        The library will appear on https://klibs.io once indexing completes. A comment will be posted here with the final status once the processing is finished.
    """.trimIndent()

    fun failure(groupId: String, artifactId: String, version: String?, reason: String) = """
        ❌ Indexing request could not be processed

        - **Group ID:** `${sanitize(groupId)}`
        - **Artifact ID:** `${sanitize(artifactId)}`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`${sanitize(version)}`"}

        **Reason:** $reason

        Please make sure the library meets the [indexing requirements](https://klibs.io/faq#how-do-i-add-a-project), then open a new issue with corrected details.

        Once you have reviewed this comment and are ready to open a new request, please close this issue.
    """.trimIndent()

    fun duplicate(groupId: String, artifactId: String, version: String?, duplicateOfIssueNumber: Int) = """
        ❌ Indexing request could not be processed

        - **Group ID:** `${sanitize(groupId)}`
        - **Artifact ID:** `${sanitize(artifactId)}`
        - **Version:** ${if (version.isNullOrBlank()) "`all versions`" else "`${sanitize(version)}`"}

        **Reason:** This request is a duplicate of #$duplicateOfIssueNumber.

        Please follow the existing request instead of opening another one.

        Once you have reviewed this comment, please close this issue.
    """.trimIndent()

    fun parseFailure() = """
        ❌ Could not read the indexing request

        We couldn't parse the input fields from this issue. Please open a new issue using the *"Request indexing of a library"* template and fill in all required fields.

        Once you have reviewed this comment and are ready to open a new request, please close this issue.
    """.trimIndent()
}