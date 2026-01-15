package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import io.klibs.app.util.BackoffProvider
import io.klibs.core.scm.repository.ScmRepositoryRepository
import org.springframework.beans.factory.annotation.Qualifier
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class GitHubRepositoryUpdatingJob(val gitHubRepositoryUpdatingService: GitHubRepositoryUpdatingService) {

    @Scheduled(initialDelay = 30, fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "updateGitHubRepositoryLock", lockAtMostFor = "30s")
    fun updateGitHubRepository() {
        LockAssert.assertLocked()
        gitHubRepositoryUpdatingService.syncRepositoryWithGitHub()
    }
}

@Service
class GitHubRepositoryUpdatingService(
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val githubIndexingService: GitHubIndexingService,
    @Value("\${klibs.integration.github.update-repos-per-iteration:3}")
    private val reposUpdatedPerCall: Int,
    @Qualifier("repoBackoffProvider")
    private val repoBackoffProvider: BackoffProvider,
) {

    fun syncRepositoryWithGitHub() {
        val reposToUpdate = scmRepositoryRepository.findMultipleForUpdate(reposUpdatedPerCall)
        if (reposToUpdate.isEmpty()) {
            logger.info("Unable to find a repo to update. Skipping.")
        }
        reposToUpdate.forEach { repoToUpdate ->
            try {
                if (repoBackoffProvider.isBackedOff(repoToUpdate.idNotNull)) {
                    logger.debug("Selected repoId=${repoToUpdate.id} ${repoToUpdate.ownerLogin}/${repoToUpdate.name} is in backoff; skipping this run")
                    return@forEach
                }
                githubIndexingService.updateRepo(repoToUpdate)
                repoBackoffProvider.onSuccess(repoToUpdate.idNotNull)
            } catch (e: Exception) {
                logger.error("Error while updating a repo", e)
                repoBackoffProvider.onFailure(repoToUpdate.idNotNull)
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubRepositoryUpdatingService::class.java)
    }
}