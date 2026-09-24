package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.repository.ScmOwnerSchedulingRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Duration

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
            schedulingRepository.deleteById(ownerToUpdate.idNotNull)
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
        val attempts = (schedulingRepository.findByIdOrNull(ownerId)?.retryAttempts ?: 0) + 1
        val reason = "${cause::class.simpleName}: ${cause.message}".take(MAX_REASON_LENGTH)
        schedulingRepository.scheduleNextRetry(ownerId, attempts, delayFor(attempts).seconds, reason)
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubOwnerUpdatingService::class.java)
        private val DELETED_DEFER = Duration.ofDays(30)
        private const val MAX_REASON_LENGTH = 500
    }
}
