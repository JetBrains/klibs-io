package io.klibs.app.job

import io.klibs.core.search.SearchService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MaterializedViewUpdatingJob(val searchService: SearchService) {

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @SchedulerLock(name = "updateMaterializedViewsLock", lockAtMostFor = "10m")
    fun updateMaterializedViews() {
        LockAssert.assertLocked();
        searchService.refreshSearchViews()
    }
}