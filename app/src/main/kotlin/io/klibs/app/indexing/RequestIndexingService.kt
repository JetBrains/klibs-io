package io.klibs.app.indexing

import io.klibs.app.util.toIndexRequest
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.search.ArtifactData
import io.klibs.integration.maven.search.impl.CentralSonatypeSearchClient
import io.klibs.integration.maven.search.paginateSearch
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
        val searchResult = paginateSearch(query)

        if (searchResult.isEmpty()) {
            throw badRequest(
                "No Kotlin Multiplatform artifacts found for $groupId${
                    artifactId?.let { ":$it" }.orEmpty()
                }"
            )
        }

        val artifactsToSave = searchResult
            .map { it.toMavenArtifact() }
            .filterNot { isAlreadyIndexedOrQueued(it) }

        if (artifactsToSave.isEmpty()) throw badRequest("All artifacts from this request are already indexed or queued")

        return artifactsToSave
    }

    private fun resolveSpecificVersion(groupId: String, artifactId: String, version: String): MavenArtifact {
        val artifact = MavenArtifact(groupId, artifactId, version, ScraperType.MANUAL_REQUEST)
        if (isAlreadyIndexedOrQueued(artifact)) throw badRequest("Artifact $groupId:$artifactId:$version is already indexed or queued")

        centralSonatypeSearchClient.getKotlinToolingMetadata(artifact)
            ?: throw badRequest(
                "Artifact $groupId:$artifactId:$version is not a valid Kotlin Multiplatform library " +
                        "(kotlin-tooling-metadata.json not found)"
            )

        return artifact
    }

    private fun isAlreadyIndexedOrQueued(artifact: MavenArtifact): Boolean =
        with(artifact) {
            when {
                packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version) != null -> {
                    logger.debug("Already indexed: $groupId:$artifactId:$version, skipping")
                    true
                }

                indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version) != null -> {
                    logger.debug("Already queued: $groupId:$artifactId:$version, skipping")
                    true
                }

                else -> false
            }
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

    private fun paginateSearch(query: Query): List<ArtifactData> =
        try {
            centralSonatypeSearchClient.paginateSearch(query).toList()
        } catch (e: Exception) {
            logger.error("Central Sonatype search failed: ${e.message}", e)
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Central Sonatype search failed"
            )
        }

    private fun saveIndexRequests(mavenArtifacts: List<MavenArtifact>) {
        val requests = mavenArtifacts.map { it.toIndexRequest() }

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