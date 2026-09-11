package io.klibs.integration.maven.service

import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

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

    suspend fun downloadIndexIfNewer(
        localIndexTimestamp: Instant,
        fetchRemoteIndexTimestamp: () -> Instant?,
    ): Instant? {
        logger.info("Checking for Maven Central index updates")

        var resultTimestamp: Instant? = null
        indexingContextManager.useCentralContext("maven-central-context") { context ->
            val remoteIndexTimestamp = fetchRemoteIndexTimestamp()

            if (remoteIndexTimestamp == null) {
                logger.warn("Local index was not updated because we couldn't extract timestamp for remote index")
                return@useCentralContext
            }

            if (remoteIndexTimestamp.isAfter(localIndexTimestamp)) {
                logger.info("New index version available (Remote: $remoteIndexTimestamp, Local: $localIndexTimestamp). Starting full index download.")
                val updateRequest = IndexUpdateRequest(context, resourceFetcher)
                updateRequest.isForceFullUpdate = true
                updateRequest.indexTempDir = indexingContextManager.getIndexTmpDir()
                updateRequest.localIndexCacheDir = indexingContextManager.getLocalIndexCacheDir()

                val result = indexUpdater.fetchAndUpdateIndex(updateRequest)

                if (result.isFullUpdate) {
                    logger.info("Full index update completed successfully")
                } else {
                    logger.warn("Index update completed, but it was not a full update as requested")
                }
                resultTimestamp = remoteIndexTimestamp
            } else {
                logger.info("Local index is up to date (Timestamp: $localIndexTimestamp). Skipping download.")
            }
        }
        return resultTimestamp
    }
}