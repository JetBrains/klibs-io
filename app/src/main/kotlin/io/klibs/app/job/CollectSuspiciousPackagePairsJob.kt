package io.klibs.app.job

import io.klibs.core.pckg.service.SuspiciousPackagePairCollector
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class CollectSuspiciousPackagePairsJob(
    private val collector: SuspiciousPackagePairCollector,
) {

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.DAYS)
    @SchedulerLock(name = "collectSuspiciousPackagePairsLock", lockAtMostFor = "1h")
    fun collectSuspiciousPackagePairs() {
        LockAssert.assertLocked()
        collector.recompute()
        logger.info("Collected suspicious package-pair candidates")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CollectSuspiciousPackagePairsJob::class.java)
    }
}
