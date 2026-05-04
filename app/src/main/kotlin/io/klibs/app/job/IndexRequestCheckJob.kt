package io.klibs.app.job

import io.klibs.app.indexing.IndexRequestCheckService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.stereotype.Component
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class IndexRequestCheckJob(private val indexRequestCheckService: IndexRequestCheckService) {

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.HOURS)
    @SchedulerLock(name = "indexRequestCheckLock", lockAtMostFor = "1h")
    fun checkIndexRequests() {
        LockAssert.assertLocked()
        indexRequestCheckService.checkIndexRequests()
    }

}