package io.klibs.integration.maven.search

import org.apache.maven.search.api.request.Query
import java.time.Instant

fun MavenSearchClient.paginateSearch(
    query: Query,
    lastUpdatedSince: Instant = Instant.EPOCH,
): Sequence<ArtifactData> = sequence {
    var page = 0
    while (true) {
        val response = searchWithThrottle(page, query, lastUpdatedSince)
        if (response.page.isEmpty()) return@sequence
        yieldAll(response.page)
        page++
    }
}
