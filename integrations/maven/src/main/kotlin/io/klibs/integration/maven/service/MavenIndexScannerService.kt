package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
     * @return A Flow of MavenArtifact objects matching the search criteria.
     */
    fun scanForNewKMPArtifacts(): Flow<MavenArtifact> = flow {
        logger.info("Starting scan of local Maven index for Kotlin tooling metadata.")

        try {
            indexingContextManager.useCentralContext("maven-central-scan-context") { context ->

                val request = createSearchRequestForKMPPackages( context)

                val resultSet = indexer.searchIterator(request)

                resultSet.use { iteratorSearchResponse ->
                    for (artifactInfo in iteratorSearchResponse) {
                        emit(
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
        context: IndexingContext
    ): IteratorSearchRequest = IteratorSearchRequest(
        BooleanQuery.Builder().add(TermQuery(Term("l", "kotlin-tooling-metadata")), BooleanClause.Occur.MUST)
            .add(TermQuery(Term("p", "json")), BooleanClause.Occur.MUST)
            .build(), context
    )
}
