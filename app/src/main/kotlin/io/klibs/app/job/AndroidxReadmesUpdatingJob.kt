package io.klibs.app.job

import io.klibs.core.readme.service.AndroidxReadmesUpdatingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class AndroidxReadmesUpdatingJob(
    private val androidxReadmesUpdatingService: AndroidxReadmesUpdatingService
) {
    @Scheduled(initialDelay = 0, fixedRate = 7, timeUnit = TimeUnit.DAYS)
    @SchedulerLock(name = "updateAndroidxReadmesLock", lockAtMostFor = "1d")
    fun updateAndroidxReadmes() {
        LockAssert.assertLocked()
        androidxReadmesUpdatingService.syncReadmes()
    }
}
