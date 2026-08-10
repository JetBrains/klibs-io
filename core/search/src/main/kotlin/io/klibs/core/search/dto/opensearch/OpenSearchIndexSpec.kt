package io.klibs.core.search.dto.opensearch

import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.search.opensearch.IndexDefinitions

/**
 * Everything one index is built from, plus the names derived from it. Each alias has exactly two
 * slots and every build targets whichever one is not currently serving, so no index accumulates:
 *
 * ```
 * alias  project-a3f9c1e2
 * slots  project-a3f9c1e2-0   project-a3f9c1e2-1
 * ```
 *
 * Readers only ever query the alias. Each build fills the idle slot and the alias is swapped onto it in
 * one atomic step, so a reader never sees a half-filled index. The hash in the alias covers settings,
 * mappings and SQL, so changing any of them starts a new alias instead of mutating the live one.
 */
class OpenSearchIndexSpec(
    val base: String,
    val settings: String,
    val mappings: String,
    val sql: String,
    val idOf: (ObjectNode) -> String,
) {

    val hash = IndexDefinitions.shortSha256Hex(settings, mappings, sql)

    val alias = "$base-$hash"

    val slots = listOf(0, 1).map { "$alias-$it" }

    /** The slot to build into: the one the alias is not serving. */
    fun idleSlot(serving: String?): String = slots[(slots.indexOf(serving) + 1) % slots.size]
}
