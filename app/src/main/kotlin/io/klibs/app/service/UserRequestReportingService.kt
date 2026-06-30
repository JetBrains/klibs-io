package io.klibs.app.service

/**
 * Publishes pending indexing-result reports back to their GitHub issues.
 */
interface UserRequestReportingService {

    /**
     * Processes the next eligible report (publishing it, dropping it, or deferring it for later).
     * @return true if a report was handled and the caller may keep draining the queue,
     * false if there is nothing eligible right now and it should stop.
     */
    fun processReportsQueue(): Boolean
}
