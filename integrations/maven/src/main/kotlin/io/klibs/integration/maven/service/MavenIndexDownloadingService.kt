package io.klibs.integration.maven.service

import org.apache.maven.index.context.IndexingContext
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for downloading the Maven Central index to a local directory for further processing.
 *
 * This service interacts with various components from the Apache Maven Indexer library to
 * fetch and update the Maven index from a remote endpoint. The downloaded index is stored locally,
 * facilitating search and analysis of Maven artifacts.
 *
 * NOTE: the current implementation always does a full index update
 */
@Service
class MavenIndexDownloadingService(
    private val indexUpdater: IndexUpdater,
    private val resourceFetcher: ResourceFetcher,
    private val indexingContextManager: MavenIndexingContextManager,
) {
    private val logger = LoggerFactory.getLogger(MavenIndexDownloadingService::class.java)

    suspend fun downloadFullIndex() {
        logger.info("Starting full index download")

        try {
            indexingContextManager.useCentralContext("maven-central-context") { context ->
                val updateRequest = IndexUpdateRequest(context, resourceFetcher)
                updateRequest.isForceFullUpdate = true

                val result = indexUpdater.fetchAndUpdateIndex(updateRequest)

                if (result.isFullUpdate) {
                    logger.info("Full index update completed successfully")
                } else {
                    logger.warn("Index update completed, but it was not a full update as requested")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download full index", e)
            throw e
        }
    }
}