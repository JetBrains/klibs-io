package io.klibs.app.indexing

import io.klibs.app.util.toIndexRequest
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import org.apache.maven.search.api.request.BooleanQuery
import org.apache.maven.search.api.request.Query
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class RequestIndexingService(
    private val centralSonatypeSearchClient: CentralSonatypeSearchClient,
    private val indexingRequestRepository: IndexingRequestRepository,
    private val packageRepository: PackageRepository,
) {
    /**
     * Discovers and saves packages for indexing after a manual request
     *
     * @param groupId Maven group ID (required)
     * @param artifactId Maven artifact ID (optional - if null, all artifacts in the group are indexed)
     * @param version Maven version (optional - if null, all versions of the artifact(s) are indexed)
     */
    @Transactional
    fun requestIndexing(groupId: String, artifactId: String?, version: String?) {
        val artifacts = discoverArtifacts(groupId, artifactId, version)
        saveIndexRequests(artifacts)
    }

    private fun discoverArtifacts(
        groupId: String,
        artifactId: String?,
        version: String?,
    ): List<MavenArtifact> {
        if (artifactId != null && version != null) {
            return listOf(resolveSpecificVersion(groupId, artifactId, version))
        }

        if (version != null) {
            logger.warn("Version is specified but artifactId is not. Ignoring version.")
        }

        val query = buildKmpQuery(groupId, artifactId)
        val results = paginateSearch(query)

        if (results.isEmpty()) {
            throw badRequest("No Kotlin Multiplatform artifacts found for $groupId${artifactId?.let{":$it"}.orEmpty()}")
        }

        return results.map { it.toMavenArtifact() }
    }

    private fun resolveSpecificVersion(groupId: String, artifactId: String, version: String): MavenArtifact {
        val metadata = centralSonatypeSearchClient.getMavenMetadata(groupId, artifactId)
            ?: throw badRequest("Artifact not found: maven-metadata.xml does not exist for $groupId:$artifactId")

        if (version !in metadata.versioning.versions) {
            throw badRequest(
                "Version $version not found in maven-metadata.xml for $groupId:$artifactId"
            )
        }

        val artifact = MavenArtifact(groupId, artifactId, version, ScraperType.MANUAL_REQUEST)
        centralSonatypeSearchClient.getKotlinToolingMetadata(artifact)
            ?: throw badRequest(
                "Artifact $groupId:$artifactId:$version is not a Kotlin Multiplatform library " +
                        "(kotlin-tooling-metadata.json not found)"
            )

        return artifact
    }

    private fun buildKmpQuery(groupId: String, artifactId: String?): Query {
        var query = BooleanQuery.and(
            Query.query("g:$groupId"),
            Query.query("l:kotlin-tooling-metadata")
        )
        if (artifactId != null) {
            query = BooleanQuery.and(query, Query.query("a:$artifactId"))
        }
        return query
    }

    private fun paginateSearch(query: Query): List<ArtifactData> {
        val results = mutableListOf<ArtifactData>()
        var currentPage = 0

        do {
            val response = try {
                centralSonatypeSearchClient.searchWithThrottle(currentPage, query)
            } catch (e: Exception) {
                logger.error("Central Sonatype search failed: ${e.message}", e)
                throw ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Central Sonatype search failed"
                )
            }
            results.addAll(response.page)
            currentPage++
        } while (response.page.isNotEmpty())

        return results
    }

    private fun saveIndexRequests(mavenArtifacts: List<MavenArtifact>) {
        val requests = mavenArtifacts.mapNotNull { artifact ->
            val (g, a, v) = Triple(artifact.groupId, artifact.artifactId, artifact.version)

            if (packageRepository.findByGroupIdAndArtifactIdAndVersion(g, a, v) != null) {
                logger.debug("Already indexed: $g:$a:$v, skipping")
                return@mapNotNull null
            }
            if (indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion(g, a, v) != null) {
                logger.debug("Already queued: $g:$a:$v, skipping")
                return@mapNotNull null
            }

            artifact.toIndexRequest()
        }

        if (requests.isEmpty()) throw badRequest("All artifacts from this request are already indexed")

        try {
            indexingRequestRepository.saveAll(requests)
            logger.info("Saved ${requests.size} index requests")
        } catch (e: Exception) {
            logger.error("Failed to save index requests: ${e.message}")

            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to save index requests"
            )
        }
    }

    private fun ArtifactData.toMavenArtifact() = MavenArtifact(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        scraperType = ScraperType.MANUAL_REQUEST,
        releasedAt = releasedAt,
    )

    private fun badRequest(message: String): ResponseStatusException =
        ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    companion object {
        private val logger = LoggerFactory.getLogger(RequestIndexingService::class.java)
    }
}