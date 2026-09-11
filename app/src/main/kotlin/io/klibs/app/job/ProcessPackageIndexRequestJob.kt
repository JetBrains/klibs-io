package io.klibs.app.job

import io.klibs.app.configuration.properties.ProcessPackageIndexingQueueProperties
import io.klibs.app.configuration.properties.ProcessPackageIndexingQueueProperties.Companion.PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX
import io.klibs.app.indexing.PackageIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    value = ["$PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX.enabled"],
    havingValue = "true",
)
class ProcessPackageIndexRequestJob(
    private val packageIndexingService: PackageIndexingService,
    private val properties: ProcessPackageIndexingQueueProperties = ProcessPackageIndexingQueueProperties(),
) {
    internal var timeProvider: () -> Long = { System.currentTimeMillis() }

    @Scheduled(
        initialDelay = 0,
        fixedRateString = "\${$PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX.fixed-rate:30m}",
    )
    @SchedulerLock(
        name = "processPackageIndexRequestsLock",
        lockAtMostFor = "\${$PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX.fixed-rate:30m}",
    )
    fun processPackageIndexRequests() {
        LockAssert.assertLocked()
        processPackageIndexQueue()
    }

    internal fun processPackageIndexQueue(
        deadline: Long = timeProvider() + properties.deadline.toMillis(),
    ): Int {
        logger.info("Processing package index queue")
        var itemsProcessed = 0
        while (timeProvider() < deadline) {
            val processed = packageIndexingService.processPackageQueue()
            if (!processed) {
                break
            }
            itemsProcessed++
        }
        logger.info("Processed $itemsProcessed requests from package index queue")
        return itemsProcessed
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessPackageIndexRequestJob::class.java)
    }
}