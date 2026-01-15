package io.klibs.app.indexing.discoverer.impl

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.service.GoogleMavenCacheService
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.androidx.GoogleMavenMasterIndexMetadata
import io.klibs.integration.maven.androidx.GoogleMavenMasterIndexMetadata.Element
import io.klibs.integration.maven.androidx.ModuleMetadataWrapper
import io.klibs.integration.maven.search.impl.GOOGLE_MAVEN_URL
import io.klibs.integration.maven.search.impl.GoogleMavenSearchClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URI


@Component
@ConditionalOnProperty(
    name = ["klibs.indexing-configuration.gmaven.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnBean(GoogleMavenCacheService::class)
class GoogleMavenPackageDiscoverer(
    private val webClient: WebClient,
    private val googleMavenSearchClient: GoogleMavenSearchClient,
    private val cacheService: GoogleMavenCacheService
) : PackageDiscoverer {
    companion object {
        private const val CONCURRENCY = 8
        private const val RETRY_COUNT = 3
        private const val BUFFER_SIZE = 10
        val MASTER_INDEX_URL: URI = URI(GOOGLE_MAVEN_URL).resolve("master-index.xml")

        private val logger: Logger = LoggerFactory.getLogger(GoogleMavenPackageDiscoverer::class.java)
    }

    private val scraperType = ScraperType.GOOGLE_MAVEN

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun discover(errorChannel: Channel<Exception>): Flow<MavenArtifact> {
        return runCatching {
            fetchMasterIndex(errorChannel)
                .map { it.name }
                .flatMapMerge(CONCURRENCY) { groupId ->
                    fetchGroupArtifacts(groupId, errorChannel)
                }
                .flatMapMerge(CONCURRENCY) { it.asFlow() }
                .buffer(BUFFER_SIZE)
        }.onFailure { e ->
            reportError(errorChannel, "Fatal error during discovery", e)
        }.getOrDefault(emptyFlow())
    }

    private suspend fun fetchMasterIndex(errorChannel: Channel<Exception>): Flow<GoogleMavenMasterIndexMetadata.Element> {
        return runCatching {
            val masterIndex = fetchWithRetry(MASTER_INDEX_URL)
                .let { GoogleMavenMasterIndexMetadata.fromXml(it) }
            masterIndex.elements.asFlow()
        }.onFailure { e ->
            reportError(errorChannel, "Failed to fetch master index", e)
        }.getOrDefault(emptyFlow())
    }

    private suspend fun fetchGroupArtifacts(
        groupId: String,
        errorChannel: Channel<Exception>
    ): Flow<List<MavenArtifact>> {
        return runCatching {
            val groupIndexUrl =
                URI(GOOGLE_MAVEN_URL).resolveDirectory(groupId.replace(".", "/")).resolve("group-index.xml")
            val fetchedContent = fetchWithRetry(groupIndexUrl)
            val fetchedGroupIndex = GoogleMavenMasterIndexMetadata.fromXml(fetchedContent)

            val cachedGroupIndex = cacheService.readGroupIndexFromCache(groupId)

            val versionsToBeFetched =
                if (cachedGroupIndex != null) collectOnlyNewVersions(fetchedGroupIndex, cachedGroupIndex, groupId)
                else fetchedGroupIndex.elements

            versionsToBeFetched.asFlow()
                .onCompletion {
                    // Cache the fetched group index after processing
                    cacheService.writeGroupIndexToCache(groupId, fetchedContent)
                }
                .map { element ->
                    processArtifactVersions(groupId, element, errorChannel)
                }

        }.onFailure { e ->
            reportError(errorChannel, "Failed to fetch group artifacts for $groupId", e)
        }.getOrDefault(emptyFlow())
    }

    private suspend fun collectOnlyNewVersions(
        fetchedGroupIndex: GoogleMavenMasterIndexMetadata,
        cachedGroupIndex: GoogleMavenMasterIndexMetadata,
        groupId: String,
    ): List<Element> {
        // Create a map of artifact name to versions for the cached group index
        val cachedArtifactVersions = cachedGroupIndex.elements.associate { element ->
            element.name to element.versions.toSet()
        }
        return fetchedGroupIndex.elements
            .mapNotNull { element ->
                val artifactId = element.name
                val cachedVersions = cachedArtifactVersions[artifactId] ?: emptySet()
                val newVersions = element.versions.filter { version -> version !in cachedVersions }

                if (newVersions.isEmpty()) {
                    null
                } else {
                    logger.debug("Found ${newVersions.size} new versions for $groupId:$artifactId")
                    val newElement = Element(
                        name = artifactId,
                        versions = newVersions
                    )
                    newElement
                }
            }
    }

    private suspend fun getArtifactMetadata(
        groupId: String,
        artifactId: String,
        version: String
    ): Result<ModuleMetadataWrapper?> {
        return runCatching {
            googleMavenSearchClient.getModuleMetadata(groupId, artifactId, version)
        }
    }

    private suspend fun processArtifactVersions(
        groupId: String,
        groupIndex: Element,
        errorChannel: Channel<Exception>
    ): List<MavenArtifact> {
        val artifactId = groupIndex.name

        return groupIndex.versions.mapNotNull { version ->
            getArtifactMetadata(groupId, artifactId, version).fold(
                onSuccess = { metadata ->
                    metadata?.let { nonNullMetadata ->
                        if (nonNullMetadata.gradleMetadata.isRootKMP()) {
                            MavenArtifact(
                                groupId = groupId,
                                artifactId = artifactId,
                                version = version,
                                scraperType = scraperType,
                                releasedAt = nonNullMetadata.releasedAt
                            )
                        } else {
                            null
                        }
                    }
                },
                onFailure = { e ->
                    reportError(errorChannel, "Failed to process artifact $groupId:$artifactId:$version", e)
                    null
                }
            )
        }
    }

    private suspend fun fetchWithRetry(uri: URI): String {
        var lastError: Throwable? = null
        repeat(RETRY_COUNT) { attempt ->
            try {
                return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .awaitSingle()
            } catch (e: WebClientResponseException) {
                lastError = e
                if (attempt < RETRY_COUNT - 1) {
                    // Exponential backoff
                    kotlinx.coroutines.delay((1L shl attempt) * 100)
                }
            }
        }
        throw lastError ?: IllegalStateException("Failed to fetch $uri after $RETRY_COUNT attempts")
    }

    private fun reportError(channel: Channel<Exception>, message: String, cause: Throwable? = null) {
        channel.trySend(Exception(message, cause))
    }

    private fun URI.resolveDirectory(dirName: String): URI {
        val dirNameCorrected = dirName.let { if (it.endsWith("/")) it else "$it/" }
        return resolve(dirNameCorrected)
    }
}
