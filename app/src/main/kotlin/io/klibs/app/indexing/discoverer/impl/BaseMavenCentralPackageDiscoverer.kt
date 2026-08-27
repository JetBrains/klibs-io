package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.indexing.discoverer.collectAllKnownMavenCentralPackages
import io.klibs.app.indexing.discoverer.createArtifactCoordinates
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.service.MavenIndexDownloadingService
import io.klibs.integration.maven.service.MavenIndexScannerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant

abstract class BaseMavenCentralPackageDiscoverer(
    private val mavenIndexDownloadingService: MavenIndexDownloadingService,
    private val mavenIndexScannerService: MavenIndexScannerService,
    private val mavenCentralScraper: MavenCentralScraper,
    private val mavenCentralLogRepository: MavenCentralLogRepository,
    private val packageRepository: PackageRepository,
    private val sourceName: String,
) : PackageDiscoverer {

    override suspend fun discover(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        var newIndexTs: Instant? = null
        try {
            val localIndexTimestamp = withContext(Dispatchers.IO) {
                mavenCentralLogRepository.retrieveMavenIndexTimestamp()
            }

            newIndexTs = mavenIndexDownloadingService.downloadIndexIfNewer(localIndexTimestamp)
        } catch (e: Exception) {
            errorChannel.send(Exception("Failed to download Maven index", e))
        }

        return if (newIndexTs != null) {
            logger.info("--- $sourceName packages discovering started in full mode. ---")
            fullDiscover(newIndexTs)
        } else {
            logger.info("--- $sourceName packages discovering started in known packages mode. ---")
            updateKnown(errorChannel)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fullDiscover(newIndexTs: Instant): Flow<MavenArtifact> {
        val existingPackages = collectAllKnownMavenCentralPackages(packageRepository)

        return mavenIndexScannerService
            .scanForNewKMPArtifacts()
            .chunked(100)
            .flatMapConcat { foundArtifactsBatch ->
                collectUnknownArtifacts(foundArtifactsBatch, existingPackages).asFlow()
            }
            .onCompletion {
                mavenCentralLogRepository.saveMavenIndexTimestamp(newIndexTs)
                logger.info(
                    "--- $sourceName packages discovering finished. Last Maven Central index timestamp changed to $newIndexTs. ---"
                )
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun updateKnown(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        val existingPackages = collectAllKnownMavenCentralPackages(packageRepository)

        return mavenCentralScraper
            .findNewVersions(existingPackages, errorChannel)
            .onCompletion {
                logger.info("--- $sourceName packages updating finished. ---")
            }
    }

    private fun collectUnknownArtifacts(
        foundArtifactsBatch: List<MavenArtifact>,
        existingPackages: Map<String, Set<String>>
    ): List<MavenArtifact> {
        return foundArtifactsBatch.filter { artifact ->
            val versions = existingPackages[artifact.getArtifactCoordinates()] ?: emptyList()
            !versions.contains(artifact.version)
        }
    }

    private fun MavenArtifact.getArtifactCoordinates(): String = createArtifactCoordinates(groupId, artifactId)

    private companion object {
        private val logger = LoggerFactory.getLogger(BaseMavenCentralPackageDiscoverer::class.java)
    }
}
