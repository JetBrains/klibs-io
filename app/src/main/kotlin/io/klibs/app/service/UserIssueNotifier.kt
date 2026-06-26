package io.klibs.app.service

/**
 * Service to notify users about the status of their indexing requests on the issue tracker.
 */
interface UserIssueNotifier {
    /**
     * Notifies the user that their indexing request was accepted and is being processed.
     */
    fun notifySuccess(issueNumber: Int)

    /**
     * Notifies the user that their indexing request could not be processed due to validation errors.
     * @param reason The reason for the failure, if available.
     */
    fun notifyFailure(issueNumber: Int, reason: String?)

    /**
     * Notifies the user that their indexing request could not be parsed.
     */
    fun notifyParseFailure(issueNumber: Int)


    /**
     * Notifies the user that a server-side error occurred while processing their request.
     * It also pings klibs.io's developer in the same message to check the problem.
     */
    fun notifyServerErrorFailure(issueNumber: Int)
}
