package io.klibs.core.owner

interface ScmOwnerSchedulingRepository {

    fun find(scmOwnerId: Int): ScmOwnerSchedulingData?

    fun clearSchedule(scmOwnerId: Int)

    fun scheduleNextRetry(
        scmOwnerId: Int,
        attempts: Int,
        backoffDelaySeconds: Long,
        reason: String
    )
}
