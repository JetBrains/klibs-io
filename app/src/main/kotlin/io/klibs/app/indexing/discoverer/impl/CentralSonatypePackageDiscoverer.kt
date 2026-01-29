package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.indexing.discoverer.collectAllKnownPackages
import io.klibs.app.indexing.discoverer.createArtifactCoordinates
import io.klibs.app.util.instant.InstantRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.scraper.MavenCentralScraper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
@ConditionalOnProperty("klibs.indexing-configuration.central-sonatype.enabled", havingValue = "true")
class CentralSonatypePackageDiscoverer(
    private val centralSonatypeScraper: MavenCentralScraper,
    private val lastPackageIndexedRepository: InstantRepository,
    private val packageRepository: PackageRepository,
    @Value("\${klibs.indexing-configuration.central-sonatype.last-updated-offset-hours:3}")
    private val lastUpdatedOffsetHours: Int,
    @Value("\${klibs.indexing-configuration.central-sonatype.mode:UPDATE_KNOWN}")
    private val mode: CentralSonatypeDiscoverMode,
) : PackageDiscoverer {

    private var lastPackageIndexTs: Instant
        get() = lastPackageIndexTsRef.get()
        set(value) {
            lastPackageIndexTsRef.set(value)
            lastPackageIndexedRepository.save(value)
        }

    private val lastPackageIndexTsRef: AtomicReference<Instant> by lazy {
        AtomicReference(
            lastPackageIndexedRepository.retrieveLatest() ?: error("Unable to retrieve the last package index timestamp")
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun discover(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        logger.info("--- Central sonatype packages discovering started in $mode mode. ---")
        return when (mode) {
            CentralSonatypeDiscoverMode.DISCOVER_NEW -> discoverNew(errorChannel)
            CentralSonatypeDiscoverMode.UPDATE_KNOWN -> updateKnown(errorChannel)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun discoverNew(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        val timestampBeforeIndexing = Instant.now()
        val existingPackages = collectAllKnownPackages(packageRepository)
        val seenCoordinates = mutableMapOf<String, MutableList<String>>()

        return centralSonatypeScraper.findKmpArtifacts(
            lastPackageIndexTs.minus(Duration.ofHours(lastUpdatedOffsetHours.toLong())),
            errorChannel
        )
            .chunked(1000)
            .flatMapConcat { foundArtifactsBatch ->
                collectUnknownArtifacts(foundArtifactsBatch, existingPackages).asFlow()
            }
            .flatMapConcat { artifact ->
                val coordinates = artifact.getArtifactCoordinates()
                if (!seenCoordinates.contains(coordinates)) {
                    seenCoordinates[coordinates] = mutableListOf()
                    centralSonatypeScraper.findAllVersionForArtifact(artifact, errorChannel)
                } else {
                    emptyFlow()
                }
            }
            // TODO KTL-2971 Indexing: `findAllVersionForArtifact` returns duplicates
            .mapNotNull { artifact ->
                if (seenCoordinates[artifact.getArtifactCoordinates()]?.contains(artifact.version) == true) {
                    null
                } else {
                    seenCoordinates[artifact.getArtifactCoordinates()]?.add(artifact.version)
                    artifact
                }
            }
            .chunked(1000)
            .flatMapMerge { foundArtifactsVersionsBatch ->
                collectUnknownArtifacts(foundArtifactsVersionsBatch, existingPackages).asFlow()
            }.onCompletion {
                lastPackageIndexTs = timestampBeforeIndexing
                logger.info("--- Central sonatype packages discovering finished. Last indexing time change to $lastPackageIndexTs ---")
            }
    }

    private suspend fun updateKnown(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        val existingPackages = collectAllKnownPackages(packageRepository)

        return centralSonatypeScraper.findNewVersions(
            existingPackages,
            errorChannel
        )
        .onCompletion {
            logger.info("--- Central sonatype packages updating finished. ---")
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
        private val logger = LoggerFactory.getLogger(CentralSonatypePackageDiscoverer::class.java)
    }
}