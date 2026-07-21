package io.klibs.app.service

import io.klibs.app.dto.UserRequestReport
import java.time.Duration

/**
 * Queue of indexing-result reports waiting to be posted back to their GitHub issues.
 *
 * Kept behind an interface so the database-backed queue can later be replaced by a
 * message broker without changing the reporting logic.
 */
interface UserRequestReportQueue {
    /**
     * Returns the next report to publish, or null when there is nothing to do.
     */
    fun poll(): UserRequestReport?

    /**
     * Hides the report from [poll] for at least [delay] so it can be retried later,
     * without consuming a retry attempt. Used when the report can't be published yet
     * (e.g. its package is not indexed).
     */
    fun deferFor(report: UserRequestReport, delay: Duration)

    /**
     * Acknowledges a report that was published successfully and removes it from the queue.
     */
    fun markAsSuccess(report: UserRequestReport)

    /**
     * Records a failed publishing attempt so the report can be retried later.
     */
    fun markAsFailed(report: UserRequestReport, errorMessage: String?)
}
