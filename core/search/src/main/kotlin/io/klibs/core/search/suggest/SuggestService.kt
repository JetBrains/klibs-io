package io.klibs.core.search.suggest

import io.klibs.core.search.SearchService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

@Service
class SuggestService(
    private val suggestRepository: SuggestRepository
) {
    /**
     * Refreshes the keyword index to include data from recently indexed / updated packages and projects.
     */
    @Scheduled(initialDelay = 2, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun refreshKeywordIndex() {
        try {
            val nanosTaken = measureNanoTime {
                suggestRepository.refreshIndex()
            }
            logger.info("Updated keywords index in ${TimeUnit.NANOSECONDS.toSeconds(nanosTaken)} seconds")
        } catch (e: Exception) {
            logger.error("Unable to refresh keywords index", e)
        }
    }

    @Transactional(readOnly = true)
    fun suggestWords(query: String?, limit: Int): List<String> {
        return suggestRepository.suggestWords(query, limit)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SuggestService::class.java)
    }
}