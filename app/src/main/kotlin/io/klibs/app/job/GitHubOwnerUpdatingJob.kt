package io.klibs.app.job

import io.klibs.app.indexing.GitHubOwnerUpdatingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
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
