package io.klibs.app.service

/**
 * Records indexing-result reports for user-originated requests so their GitHub issues can be notified.
 */
interface UserRequestReportWriter {
    /**
     * Records a SUCCESS report for a user-originated request.
     * No-op for requests not originating from a user issue.
     */
    fun saveSuccessReport(indexRequestId: Long)

    /**
     * Records a FAILURE report only once retries are exhausted.
     * No-op for requests not originating from a user issue.
     */
    fun saveFailureReportIfTerminal(indexRequestId: Long, errorMessage: String?)
}
