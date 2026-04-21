package io.klibs.app.job

import io.klibs.core.pckg.service.PackageService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PackageVersionTypeJob(private val packageService: PackageService) {

    private val logger = LoggerFactory.getLogger(PackageVersionTypeJob::class.java)

    @Scheduled(initialDelay = 10, fixedDelay = Long.MAX_VALUE, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "fillPackageVersionTypeLock", lockAtLeastFor = "365d", lockAtMostFor = "365d")
    fun fillVersionType() {
        LockAssert.assertLocked()
        logger.info("=== Starting one-time job to fill version_type for packages ===")

        val totalUpdated = packageService.fillAllNullVersionTypes()

        logger.info("=== Finished filling version_type for $totalUpdated packages ===")
    }

}
