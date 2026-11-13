package io.klibs.app.job

import io.klibs.app.indexing.AndroidxRepositoryUpdatingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class AndroidxRepositoryUpdatingJob(
    private val androidxRepositoryUpdatingService: AndroidxRepositoryUpdatingService
) {
    @Scheduled(initialDelay = 0, fixedRate = 7, timeUnit = TimeUnit.DAYS)
    @SchedulerLock(name = "updateAndroidxRepositoryLock", lockAtMostFor = "7d")
    fun updateAndroidxRepository() {
        LockAssert.assertLocked()
        androidxRepositoryUpdatingService.syncReadmes()
    }
}
