package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenIntegrationProperties
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.context.IndexingContext
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

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
    private val properties: MavenIntegrationProperties,
    private val indexer: Indexer,
    private val indexUpdater: IndexUpdater,
    private val indexCreators: List<IndexCreator>,
    private val resourceFetcher: ResourceFetcher,
) {
    private val logger = LoggerFactory.getLogger(MavenIndexDownloadingService::class.java)

    fun downloadFullIndex() {
        val centralConfig = properties.central
        val indexDir = File(centralConfig.indexDir)
        if (!indexDir.exists()) {
            indexDir.mkdirs()
        }

        logger.info("Starting full index download from {} to {}", centralConfig.indexEndpoint, indexDir.absolutePath)

        var context: IndexingContext? = null
        try {
            context = indexer.createIndexingContext(
                "maven-central-context",
                "central",
                indexDir,
                indexDir,
                centralConfig.indexEndpoint,
                null,
                true,
                true,
                indexCreators
            )

            val updateRequest = IndexUpdateRequest(context, resourceFetcher)
            updateRequest.isForceFullUpdate = true

            val result = indexUpdater.fetchAndUpdateIndex(updateRequest)

            if (result.isFullUpdate) {
                logger.info("Full index update completed successfully")
            } else {
                logger.warn("Index update completed, but it was not a full update as requested")
            }
        } catch (e: Exception) {
            logger.error("Failed to download full index", e)
            throw e
        } finally {
            context?.let {
                indexer.closeIndexingContext(it, false)
            }
        }
    }
}