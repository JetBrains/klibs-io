package io.klibs.integration.maven.scraper.impl

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.scraper.MavenCentralScraper
import io.klibs.integration.maven.search.MavenSearchClient
import io.klibs.integration.maven.search.impl.BaseMavenSearchClient
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.apache.maven.search.api.request.BooleanQuery
import org.apache.maven.search.api.request.Query
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ConditionalOnProperty(
    name = ["klibs.indexing-configuration.central-sonatype.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class CentralSonatypeScraper(
    private val discoveryCentralSonatypeSearchClient: MavenSearchClient,
    private val centralSonatypeSearchClient: CentralSonatypeSearchClient
) : MavenCentralScraper {
    override val scraperType: ScraperType = ScraperType.CENTRAL_SONATYPE

    private fun createScrapeQuery(): Query {
        return Query.query("l:kotlin-tooling-metadata")
    }

    private fun createFindArtifactVersionsQuery(groupId: String, artifactId: String): Query {
        return BooleanQuery.and(
            Query.query("g:$groupId"),
            Query.query("a:$artifactId")
        )
    }

    override suspend fun findKmpArtifacts(
        lastUpdatedSince: Instant,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact> = flow {
        val query = createScrapeQuery()
        executeFindKmpArtifactsQuery(discoveryCentralSonatypeSearchClient, query, lastUpdatedSince, errorChannel)
    }

    override suspend fun findAllVersionForArtifact(
        mavenArtifact: MavenArtifact,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact> = flow {
        val searchQuery = createFindArtifactVersionsQuery(mavenArtifact.groupId, mavenArtifact.artifactId)
        executeFindAllVersionForArtifactQuery(centralSonatypeSearchClient, searchQuery, errorChannel = errorChannel)
    }

    private suspend fun FlowCollector<MavenArtifact>.executeFindKmpArtifactsQuery(
        client: MavenSearchClient,
        query: Query,
        lastUpdatedSince: Instant,
        errorChannel: Channel<Exception>
    ) {
        var currentPage = 0
        var pageCount = 0
        do {
            runCatching {
                val response = client.searchWithThrottle(currentPage, query, lastUpdatedSince)

                val artifacts = response.page.map {
                    MavenArtifact(
                        it.groupId,
                        it.artifactId,
                        it.version,
                        scraperType,
                        it.releasedAt
                    )
                }
                for (artifact in artifacts) {
                    emit(artifact)
                }
                pageCount = response.page.size
            }.onFailure { exception ->
                errorChannel.send(
                    Exception("Could not process request for artifacts: $query", exception)
                )
            }
            currentPage++
        } while (pageCount > 0)
    }

    private suspend fun FlowCollector<MavenArtifact>.executeFindAllVersionForArtifactQuery(
        client: BaseMavenSearchClient,
        query: Query,
        errorChannel: Channel<Exception>
    ) {
        var currentPage = 0
        var totalHits = 0
        var currentHits = 0
        var processedArtifactsCount = 0
        do {
            runCatching {
                val response = client.searchWithThrottle(currentPage, query)

                val artifacts = response.page.map {
                    MavenArtifact(
                        it.groupId,
                        it.artifactId,
                        it.version,
                        scraperType,
                        it.releasedAt
                    )
                }
                for (artifact in artifacts) {
                    emit(artifact)
                }
                totalHits = response.totalHits

                currentHits = response.currentHits
            }.onFailure { exception ->
                errorChannel.send(
                    Exception("Could not process request for artifacts: $query", exception)
                )
            }
            processedArtifactsCount = client.pageSize() * currentPage + currentHits
            currentPage++
        } while (totalHits > processedArtifactsCount)
    }

    override suspend fun findNewVersions(
        knownArtifacts: Map<String, Set<String>>,
        errorChannel: Channel<Exception>
    ): Flow<MavenArtifact> = flow {
        for ((coordinates, knownVersions) in knownArtifacts) {
            runCatching {
                val parts = coordinates.split(":")
                if (parts.size != 2) return@runCatching
                val groupId = parts[0]
                val artifactId = parts[1]

                val metadata = centralSonatypeSearchClient.getMavenMetadata(groupId, artifactId)
                if (metadata != null) {
                    val newVersions = metadata.versioning.versions.filter { it !in knownVersions }
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
            }.onFailure { exception ->
                errorChannel.send(
                    Exception("Could not process request for metadata of $coordinates", exception)
                )
            }
        }
    }
}