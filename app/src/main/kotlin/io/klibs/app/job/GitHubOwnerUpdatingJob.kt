package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class GitHubOwnerUpdatingJob(val githubIndexingService: GitHubIndexingService) {

    @Scheduled(initialDelay = 30, fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "updateGitHubOwnerLock", lockAtMostFor = "30s")
    fun updateGitHubOwner() {
        LockAssert.assertLocked();
        githubIndexingService.syncOwnerWithGitHub()
    }
}