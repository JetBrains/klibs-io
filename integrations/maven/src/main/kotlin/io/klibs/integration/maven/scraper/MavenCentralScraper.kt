package io.klibs.integration.maven.scraper

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Interface for scrapers used to interact with Maven Central repositories to discover and fetch metadata
 * about Kotlin Multiplatform (KMP) artifacts and their versions.
 */
interface MavenCentralScraper {
    /**
     * Fetches Kotlin Multiplatform (KMP) artifacts information from Maven Central that have been updated or
     * added since the specified timestamp. Includes functionality to report errors encountered
     * during the discovery process.
     *
     * @param lastUpdatedSince The timestamp indicating the earliest update time for artifacts to be discovered.
     * @param errorChannel A channel used to report errors encountered during the discovery process.
     * @return A flow emitting discovered Maven artifacts.
     */
    suspend fun findKmpArtifacts(
        lastUpdatedSince: Instant,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact>

    /**
     * Fetches all available versions of a specified Maven artifact from a Maven Central repository.
     * Any errors encountered during the process are sent to the provided error channel.
     *
     * @param mavenArtifact The Maven artifact for which to fetch all available versions.
     * @param errorChannel A channel used to report errors encountered during version discovery.
     * @return A flow emitting Maven artifacts, each representing a version of the given artifact.
     */
    suspend fun findAllVersionForArtifact(
        mavenArtifact: MavenArtifact,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact>


    /**
     * Specifies the type of scraper used for interacting with Maven Central repositories.
     * Determines the source or method used to query or fetch artifact metadata.
     *
     * The chosen scraper type affects the underlying implementation of metadata retrieval strategies.
     */
    val scraperType: ScraperType
}
