package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(ProcessPackageIndexingQueueProperties.PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX)
data class ProcessPackageIndexingQueueProperties(
    val enabled: Boolean = true,
    val deadline: Duration = Duration.ofMinutes(29),
    val fixedRate: Duration = Duration.ofMinutes(30),
) {
    companion object {
        const val PROCESS_PACKAGE_INDEXING_QUEUE_PREFIX = "klibs.scheduling.process-indexing-queue"
    }
}
