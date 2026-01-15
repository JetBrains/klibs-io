package io.klibs.integration.maven.search

import org.apache.maven.search.api.request.Query
import java.time.Instant

/**
 * Represents a client, which is responsible for executing search operations with throttle control with Maven.
 */
interface MavenSearchClient
{
    fun pageSize(): Int

    fun searchWithThrottle(page: Int, query: Query, lastUpdatedSince: Instant = Instant.EPOCH): MavenSearchResponse
}

data class MavenSearchResponse(
    val totalHits: Int,
    val currentHits: Int,
    val page: List<ArtifactData>,
)

data class ArtifactData(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val releasedAt: Instant? = null,
)