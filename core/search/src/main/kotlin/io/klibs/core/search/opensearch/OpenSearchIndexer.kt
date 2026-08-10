package io.klibs.core.search.opensearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import org.opensearch.client.json.JsonpDeserializer
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.io.StringReader

/**
 * Rebuilds into whichever of an alias's two slots is idle and swaps the alias onto it, so readers
 * never see a partial index and mapping changes ship as code. The slot the alias moves off is kept
 * until the next build overwrites it, which is the rollback window.
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchIndexer(
    private val client: OpenSearchClient,
    private val jacksonJsonpMapper: JacksonJsonpMapper,
    private val jdbcClient: JdbcClient,
    private val mapper: ObjectMapper,
) {

    fun aliasExists(spec: OpenSearchIndexSpec): Boolean =
        client.indices().existsAlias { it.name(spec.alias) }.value()

    fun sync(spec: OpenSearchIndexSpec) {
        val (oldIndex, newIndex) = createNewIndex(spec)

        val rows = fillNewIndexWithFreshData(spec, newIndex)

        swapAlias(spec, newIndex, oldIndex)
        log.info("swapped alias '{}' onto '{}' with {} docs", spec.alias, newIndex, rows.size)
    }

    private fun createNewIndex(spec: OpenSearchIndexSpec): Pair<String?, String> {
        val indices = servingIndices(spec.alias)
        check(indices.size <= 1) {
            "alias '${spec.alias}' points at ${indices.size} indices $indices; " +
                    "refusing to rebuild until it points at one"
        }
        val serving = indices.singleOrNull()

        val target = spec.idleSlot(serving)
        client.indices().delete { it.index(target).ignoreUnavailable(true) }

        client.indices().create {
            it.index(target)
                .settings(parse(spec.settings, IndexSettings._DESERIALIZER))
                .mappings(parse(withMeta(spec.mappings, spec.hash), TypeMapping._DESERIALIZER))
        }
        return Pair(serving, target)
    }

    private fun fillNewIndexWithFreshData(
        spec: OpenSearchIndexSpec,
        newIndex: String
    ): List<String?> {
        val rows = jdbcClient.sql(spec.sql).query(String::class.java).list()
        check(rows.isNotEmpty()) { "projection for '${spec.alias}' returned no rows; refusing to swap onto an empty index" }

        // Parse per batch rather than up front: the whole projection as ObjectNodes is several times
        // its ~9MB of JSON, and only one batch is ever needed at a time.
        rows.chunked(BATCH).forEach { chunk ->
            bulkIndex(newIndex, chunk.map { mapper.readTree(it) as ObjectNode }, spec.idOf)
        }
        // Bulk-written docs aren't searchable until a refresh (default interval 1s).
        // Force it, so alias can swap successfully.
        client.indices().refresh { it.index(newIndex) }
        return rows
    }

    private fun bulkIndex(index: String, batch: List<ObjectNode>, idOf: (ObjectNode) -> String) {
        val request = BulkRequest.Builder()
        batch.forEach { node ->
            request.operations { op -> op.index { it.index(index).id(idOf(node)).document(node) } }
        }
        val response = client.bulk(request.build())
        check(!response.errors()) {
            "bulk into '$index' had errors: ${response.items().firstOrNull { it.error() != null }?.error()?.reason()}"
        }
    }

    private fun swapAlias(spec: OpenSearchIndexSpec, target: String, serving: String?) {
        client.indices().updateAliases { updateRequest ->
            serving?.let { old ->
                updateRequest.actions { a -> a.remove { it.index(old).alias(spec.alias) } }
            }
            updateRequest.actions { a -> a.add { it.index(target).alias(spec.alias) } }
        }
    }

    private fun servingIndices(alias: String): Set<String> {
        val indicesClient = client.indices()
        if (!indicesClient.existsAlias { it.name(alias) }.value()) return emptySet()

        val aliasesByIndex = indicesClient.getAlias { it.name(alias) }.result()
        return aliasesByIndex.keys
    }

    /** Puts mappings hash into the _meta of the index so "which mappings served this" stays answerable. */
    private fun withMeta(mappingsJson: String, hash: String): String {
        val mappings = mapper.readTree(mappingsJson) as ObjectNode
        mappings.putObject("_meta").put("mappings_hash", hash)
        return mappings.toString()
    }

    private fun <T> parse(json: String, deserializer: JsonpDeserializer<T>): T =
        jacksonJsonpMapper.jsonProvider().createParser(StringReader(json))
            .use { deserializer.deserialize(it, jacksonJsonpMapper) }

    private companion object {
        private val log = LoggerFactory.getLogger(OpenSearchIndexer::class.java)
        private const val BATCH = 500
    }
}
