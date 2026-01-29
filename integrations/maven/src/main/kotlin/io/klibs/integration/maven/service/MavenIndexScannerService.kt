package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.maven.index.*
import org.apache.maven.index.context.IndexingContext
import  org.apache.lucene.search.BooleanQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for scanning the local Maven index for artifacts.
 */
@Service
class MavenIndexScannerService(
    private val indexer: Indexer,
    private val indexingContextManager: MavenIndexingContextManager,
) {
    private val logger = LoggerFactory.getLogger(MavenIndexScannerService::class.java)

    /**
     * Scans the local Maven index for artifacts containing Kotlin tooling metadata.
     * The scan is performed iteratively to keep memory usage low.
     *
     * @param from Lower border for the last modified timestamp.
     * @param to Top border for the last modified timestamp.
     * @return A Flow of MavenArtifact objects matching the search criteria.
     */
    fun scanForKotlinToolingMetadata(from: Instant, to: Instant): Flow<MavenArtifact> = channelFlow {
        logger.info("Starting scan of local Maven index for Kotlin tooling metadata from {} to {}", from, to)

        try {
            indexingContextManager.useCentralContext("maven-central-scan-context") { context ->
                val fromMillis = from.toEpochMilli().toString()
                val toMillis = to.toEpochMilli().toString()

                val request = createSearchRequestForKMPPackages(fromMillis, toMillis, context)

                val resultSet = indexer.searchIterator(request)

                resultSet.use { iteratorSerachResponse ->
                    for (artifactInfo in iteratorSerachResponse) {
                        send(
                            MavenArtifact(
                                groupId = artifactInfo.groupId,
                                artifactId = artifactInfo.artifactId,
                                version = artifactInfo.version,
                                scraperType = ScraperType.CENTRAL_SONATYPE,
                                releasedAt = Instant.ofEpochMilli(artifactInfo.lastModified)
                            )
                        )
                    }
                    logger.info("Local index scan completed.")
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while scanning local Maven index", e)
            throw e
        }
    }

    private fun createSearchRequestForKMPPackages(
        fromMillis: String,
        toMillis: String,
        context: IndexingContext
    ): IteratorSearchRequest = IteratorSearchRequest(
        BooleanQuery.Builder().add(TermQuery(Term("l", "kotlin-tooling-metadata")), BooleanClause.Occur.MUST)
            .add(TermRangeQuery.newStringRange("m", fromMillis, toMillis, true, true), BooleanClause.Occur.MUST)
            .add(TermQuery(Term("p", "json")), BooleanClause.Occur.MUST)
            .build(), context
    )
}
