package io.klibs.integration.maven.search

import org.apache.maven.search.api.request.Query
import java.time.Instant

/**
 * Paginates through all results of [query] lazily, one page at a time.
 *
 * Iteration stops once [MavenSearchClient.searchWithThrottle] returns an empty page.
 * Any exception thrown by the underlying client propagates to the caller — wrap iteration
 * in whichever error handling (error channel, rethrow as HTTP error, etc.) fits the context.
 */
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
