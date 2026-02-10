package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenIntegrationProperties
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.ResourceFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

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
    properties: MavenIntegrationProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(MavenIndexDownloadingService::class.java)
    private val restClient = restClientBuilder
        .baseUrl("${properties.central.indexEndpoint}/.index/")
        .build()

    suspend fun downloadIndexIfNewer(localIndexTimestamp: Instant): Instant? {
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

    private fun fetchRemoteIndexTimestamp(): Instant? {
        return try {
            restClient.get()
                .uri("nexus-maven-repository-index.properties")
                .retrieve()
                .body<String>()?.let { content ->
                    val props = Properties()
                    props.load(content.reader())
                    val indexTimestamp = props.getProperty("nexus.index.timestamp")
                    if (indexTimestamp != null) {
                        // Maven index timestamp format: yyyyMMddHHmmss.SSS Z
                        SimpleDateFormat("yyyyMMddHHmmss.SSS Z").parse(indexTimestamp).toInstant()
                    } else null
                }
        } catch (e: Exception) {
            logger.error("Could not fetch remote index timestamp", e)
            null
        }
    }
}