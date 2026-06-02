package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import io.klibs.app.util.BackoffProvider
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.ScmRepositorySchedulingRepository
import io.klibs.core.scm.repository.health.OssHealthIssueOrPrSyncService
import io.klibs.core.scm.repository.health.OssHealthScoreService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class GitHubRepositoryUpdatingJob(val gitHubRepositoryUpdatingService: GitHubRepositoryUpdatingService) {

    @Scheduled(initialDelay = 30, fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "updateGitHubRepositoryLock", lockAtMostFor = "10m")
    fun updateGitHubRepository() {
        LockAssert.assertLocked()
        gitHubRepositoryUpdatingService.syncRepositoryWithGitHub()
    }
}

@Service
class GitHubRepositoryUpdatingService(
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val schedulingRepository: ScmRepositorySchedulingRepository,
    private val githubIndexingService: GitHubIndexingService,
    private val ossHealthIssueOrPrSyncService: OssHealthIssueOrPrSyncService,
    private val ossHealthScoreService: OssHealthScoreService,
    @Value("\${klibs.integration.github.update-repos-per-iteration:3}")
    private val reposUpdatedPerCall: Int
) {

    fun syncRepositoryWithGitHub() {
        val reposToUpdate = scmRepositoryRepository.findMultipleForUpdate(reposUpdatedPerCall)
        if (reposToUpdate.isEmpty()) {
            logger.info("Unable to find a repo to update. Skipping.")
        }
        reposToUpdate.forEach { repoToUpdate ->
            try {
                githubIndexingService.updateRepo(repoToUpdate)
                schedulingRepository.clearSchedule(repoToUpdate.idNotNull)
            } catch (e: Exception) {
                logger.error("Error while updating a repo", e)
                val attempts = (schedulingRepository.find(repoToUpdate.idNotNull)?.retryAttempts ?: 0) + 1
                val backoffDelay = BackoffProvider.computeBackoffDelay(
                    base = 60L,
                    exp = 3L,
                    attempts = attempts
                )
                schedulingRepository.scheduleNextRetry(repoToUpdate.idNotNull, attempts, backoffDelay.seconds)
                return@forEach
            }

            val now = Instant.now()
            runOssIssueOrPrSyncIfDue(repoToUpdate, now)
            runOssScoreIfDue(repoToUpdate, now)
        }
    }

    private fun runOssIssueOrPrSyncIfDue(repo: ScmRepositoryEntity, now: Instant) {
        if (!ossHealthIssueOrPrSyncService.isDue(repo, now)) return
        try {
            ossHealthIssueOrPrSyncService.syncOne(repo, now)
        } catch (e: Exception) {
            logger.warn("OSS issue/PR sync failed for ${repo.ownerLogin}/${repo.name}: ${e.message}")
        }
    }

    private fun runOssScoreIfDue(repo: ScmRepositoryEntity, now: Instant) {
        if (!ossHealthScoreService.isDue(repo, now)) return
        try {
            ossHealthScoreService.computeOne(repo, now)
        } catch (e: Exception) {
            logger.warn("OSS score compute failed for ${repo.ownerLogin}/${repo.name}: ${e.message}")
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubRepositoryUpdatingService::class.java)
    }
}