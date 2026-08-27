package io.klibs.integration.maven.scraper.impl

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.exception.MavenRateLimitedException
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.maven.search.api.request.Query
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["klibs.indexing-configuration.central-sonatype.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class CentralSonatypeScraper(
    private val centralSonatypeSearchClient: CentralSonatypeSearchClient
) : MavenCentralScraper {
    override val scraperType: ScraperType = ScraperType.CENTRAL_SONATYPE

    override suspend fun findNewVersions(
        knownArtifacts: Map<String, Set<String>>,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact> = flow {
        for ((coordinates, knownVersions) in knownArtifacts) {
            val exception = runCatching {
                val parts = coordinates.split(":")
                if (parts.size != 2) return@runCatching
                val groupId = parts[0]
                val artifactId = parts[1]

                val metadata = centralSonatypeSearchClient.getMavenMetadata(groupId, artifactId)
                if (metadata != null) {
                    val newVersions = metadata.versioning.versions.filter { it !in knownVersions }

                    logger.trace("Found ${newVersions.size} new versions for $coordinates")
                    for (version in newVersions) {
                        emit(
                            MavenArtifact(
                                groupId = groupId,
                                artifactId = artifactId,
                                version = version,
                                scraperType = scraperType,
                                releasedAt = null
                            )
                        )
                    }
                }
            }.exceptionOrNull() ?: continue

            errorChannel.send(
                Exception("Could not process request for metadata of $coordinates", exception)
            )
            if (exception is MavenRateLimitedException) {
                logger.warn("Rate limited by Maven Central, stopping new version discovery")
                break
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(CentralSonatypeScraper::class.java)
    }
}