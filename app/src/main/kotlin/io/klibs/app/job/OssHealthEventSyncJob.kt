package io.klibs.app.job

import io.klibs.app.oss_health.OssHealthEventSyncService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class OssHealthEventSyncJob(
    private val ossHealthEventSyncService: OssHealthEventSyncService,
) {

    @Scheduled(initialDelay = 120, fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "ossHealthEventSyncLock", lockAtMostFor = "5m")
    fun sync() {
        LockAssert.assertLocked()
        ossHealthEventSyncService.syncOldestRepo()
    }
}
