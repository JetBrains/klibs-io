package io.klibs.app.job

import io.klibs.app.indexing.ProjectIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit


@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class AddAiMetadataForProjectJob(val projectIndexingService: ProjectIndexingService) {

    @Scheduled(initialDelay = 2, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    @SchedulerLock(name = "addAiDescriptionLock", lockAtMostFor = "23h")
    fun addAiDescription() {
        LockAssert.assertLocked()
        projectIndexingService.addAiDescription()
    }

    // Scheduled timing is like that so that intersection between jobs would be minimal.
    // That way, resources consumption would be more evenly distributed.
    @Scheduled(initialDelay = 90, fixedRate = 65, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "addAiTags", lockAtMostFor = "23h")
    fun addAiTags() {
        LockAssert.assertLocked()
        projectIndexingService.addAiTags()
    }
}