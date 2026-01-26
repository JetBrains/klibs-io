package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.indexing.discoverer.collectAllKnownPackages
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.scraper.MavenCentralScraper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("klibs.indexing-configuration.updater.enabled", havingValue = "true")
class MavenCentralPackageUpdater(
    private val centralSonatypeScraper: MavenCentralScraper,
    private val packageRepository: PackageRepository,
): PackageDiscoverer {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun discover(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        logger.info("--- Central sonatype packages discovering started. ---")
        val existingPackages = collectAllKnownPackages(packageRepository)

        return centralSonatypeScraper.findNewVersions(
            existingPackages,
            errorChannel
        )
            .onCompletion {
                logger.info("--- Central sonatype packages discovering finished. ---")
            }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(MavenCentralPackageUpdater::class.java)
    }
}