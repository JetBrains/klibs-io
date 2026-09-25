package io.klibs.app.job

import io.klibs.app.service.SuspiciousForkBanService
import io.klibs.core.pckg.service.SuspiciousPackageCandidateCollectionService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class SuspiciousPackageCandidatesJob(
    private val candidateCollectionService: SuspiciousPackageCandidateCollectionService,
    private val suspiciousForkBanService: SuspiciousForkBanService,
) {

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "suspiciousPackageCandidatesLock", lockAtMostFor = "1h")
    fun collectAndBan() {
        LockAssert.assertLocked()
        val refresh = candidateCollectionService.refreshCandidates()
        val bans = suspiciousForkBanService.banConfirmedForks()
        logger.info(
            "Collected and banned: inserted={}, deleted={}, evaluated={}, banned={}, notFork={}, noDecision={}",
            refresh.inserted,
            refresh.deleted,
            bans.evaluated,
            bans.banned,
            bans.notFork,
            bans.noDecision,
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousPackageCandidatesJob::class.java)
    }
}
