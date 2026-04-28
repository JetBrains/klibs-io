package io.klibs.app.job

import io.klibs.app.oss_health.OssHealthScoreService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class OssHealthScoreJob(
    private val ossHealthScoreService: OssHealthScoreService,
) {

    @Scheduled(initialDelay = 150, fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "ossHealthScoreLock", lockAtMostFor = "5m")
    fun compute() {
        LockAssert.assertLocked()
        ossHealthScoreService.computeOldestRepo()
    }
}
