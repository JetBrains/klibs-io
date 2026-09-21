package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import io.klibs.app.indexing.ScmOwnerDeletedException
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerSchedulingRepository
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class GitHubOwnerUpdatingJob(val gitHubOwnerUpdatingService: GitHubOwnerUpdatingService) {

    @Scheduled(initialDelay = 30, fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "updateGitHubOwnerLock", lockAtMostFor = "30s")
    fun updateGitHubOwner() {
        LockAssert.assertLocked()
        gitHubOwnerUpdatingService.syncOwnerWithGitHub()
    }
}

@Service
class GitHubOwnerUpdatingService(
    private val scmOwnerRepository: ScmOwnerRepository,
    private val schedulingRepository: ScmOwnerSchedulingRepository,
    private val githubIndexingService: GitHubIndexingService
) {

    fun syncOwnerWithGitHub() {
        val ownerToUpdate = scmOwnerRepository.findForUpdate() ?: return

        try {
            val updated = githubIndexingService.updateOwner(ownerToUpdate)
            logger.debug("Updated GitHub owner: {}", updated)
            schedulingRepository.clearSchedule(ownerToUpdate.idNotNull)
        } catch (e: ScmOwnerDeletedException) {
            logger.warn("Deferring a deleted GitHub owner: {}", e.message)
            scheduleNextRetry(ownerToUpdate.idNotNull, e) { DELETED_DEFER }
        } catch (e: Exception) {
            logger.error("Error while updating a GitHub owner", e)
            scheduleNextRetry(ownerToUpdate.idNotNull, e) { attempts ->
                BackoffProvider.computeBackoffDelay(base = 60L, exp = 2L, attempts = attempts)
            }
        }
    }

    private fun scheduleNextRetry(ownerId: Int, cause: Exception, delayFor: (attempts: Int) -> Duration) {
        val attempts = (schedulingRepository.find(ownerId)?.retryAttempts ?: 0) + 1
        val reason = "${cause::class.simpleName}: ${cause.message}".take(MAX_REASON_LENGTH)
        schedulingRepository.scheduleNextRetry(ownerId, attempts, delayFor(attempts).seconds, reason)
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubOwnerUpdatingService::class.java)
        private val DELETED_DEFER = Duration.ofDays(30)
        private const val MAX_REASON_LENGTH = 500
    }
}
