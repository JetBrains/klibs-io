package io.klibs.app.indexing

import io.klibs.core.project.ANDROIDX_PREFIX
import io.klibs.core.readme.service.AndroidxReadmesUpdatingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AndroidxReadmeSyncListener(
    private val androidxReadmesUpdatingService: AndroidxReadmesUpdatingService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAndroidxProjectCreated(event: AndroidxProjectCreatedEvent) {
        runCatching { androidxReadmesUpdatingService.syncReadme(event.project) }
            .onFailure { logger.error("Failed to sync readme for new project $ANDROIDX_PREFIX${event.project.name}", it) }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AndroidxReadmeSyncListener::class.java)
    }
}
