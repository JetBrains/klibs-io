package io.klibs.app.job

import io.klibs.app.service.UserRequestReportingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class UserRequestReportingJob(private val userRequestReportingService: UserRequestReportingService) {

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @SchedulerLock(name = "userRequestReportingLock", lockAtMostFor = "5m")
    fun reportUserRequestStatuses() {
        LockAssert.assertLocked()
        while (userRequestReportingService.processReportsQueue()) {
        }
    }
}
