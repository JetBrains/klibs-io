package io.klibs.app.job

import io.klibs.app.indexing.UserRequestCheckService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.stereotype.Component
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class UserRequestCheckJob(private val userRequestCheckService: UserRequestCheckService) {

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.HOURS)
    @SchedulerLock(name = "userRequestCheckLock", lockAtMostFor = "1h")
    fun checkUserRequests() {
        LockAssert.assertLocked()
        userRequestCheckService.checkUserRequests()
    }

}