package io.klibs.app.job

import io.klibs.app.indexing.PackageIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["klibs.indexing", "klibs.ai"], havingValue = "true")
class IndexNewPackagesJob(val packageIndexingService: PackageIndexingService) {

    @Scheduled(initialDelay = 0) // Every day at 2AM
    fun indexNewPackages() {
//        LockAssert.assertLocked()
        packageIndexingService.indexNewPackages()
    }
}