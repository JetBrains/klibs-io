package io.klibs.app.job

import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class CollectSuspiciousPackageCandidatesJob(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
) {

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.DAYS)
    @SchedulerLock(name = "collectSuspiciousPackageCandidatesLock", lockAtMostFor = "10m")
    fun collectCandidates() {
        LockAssert.assertLocked()
        val inserted = suspiciousPackageCandidateRepository.insertMissingCandidates()
        logger.info(
            "Collected suspicious package candidates: inserted={}, total={}",
            inserted,
            suspiciousPackageCandidateRepository.count(),
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CollectSuspiciousPackageCandidatesJob::class.java)
    }
}
