package io.klibs.app.job

import io.klibs.core.project.repository.ProjectRepository
import io.micrometer.core.annotation.Timed
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RefreshDependentCountJob(
    private val projectRepository: ProjectRepository,
) {

    @Scheduled(initialDelay = 0, fixedRate = 6, timeUnit = TimeUnit.HOURS)
    @SchedulerLock(name = "refreshDependentCountLock", lockAtMostFor = "1h")
    @Timed(
        value = "klibs.project.refresh_dependent_count.time",
        description = "Klibs: Time taken to refresh project.dependent_count",
    )
    fun refreshDependentCounts() {
        LockAssert.assertLocked()
        projectRepository.recomputeAllDependentCounts()
        logger.info("Refreshed project.dependent_count")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RefreshDependentCountJob::class.java)
    }
}
