package io.klibs.core.scm.repository

/**
 * Persistence for per-repo update backoff state (`scm_repo_scheduling`).
 *
 * A row exists only while a repo is backed off after a failed update; its absence means the repo is
 * eligible now. The backoff *policy* (how long to wait for a given attempt count) lives in the app
 * layer — this interface only reads and writes the state.
 */
interface ScmRepositorySchedulingRepository {

    /** Current scheduling state for a repo, or `null` if it has none (i.e. eligible now). */
    fun find(scmRepoId: Int): ScmRepositorySchedulingData?

    /** Clears backoff state after a successful update by removing the row, so the repo is eligible again. */
    fun clearSchedule(scmRepoId: Int)

    /**
     * Records a failed update: stores the new [attempts] count and pushes the next eligible time
     * [backoffDelaySeconds] into the future. Upserts, since the first failure has no existing row.
     */
    fun scheduleNextRetry(scmRepoId: Int, attempts: Int, backoffDelaySeconds: Long)
}
