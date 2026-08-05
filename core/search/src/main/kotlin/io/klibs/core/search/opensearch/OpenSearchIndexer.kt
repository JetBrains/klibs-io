package io.klibs.core.search.opensearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.opensearch.client.json.JsonpDeserializer
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.IndexState
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.io.StringReader
import java.time.Instant

/**
 * Builds a fresh timestamped index per cycle and swaps the alias onto it, so readers
 * never see a partial index and mapping changes ship as code.
 *
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchIndexer(
    private val client: OpenSearchClient,
    private val jacksonJsonpMapper: JacksonJsonpMapper,
    private val jdbcClient: JdbcClient,
    private val properties: OpenSearchProperties,
    private val mapper: ObjectMapper,
) {

    fun aliasExists(spec: IndexSpec): Boolean =
        client.indices().existsAlias { it.name(spec.alias) }.value()

    fun sync(spec: IndexSpec, now: Instant = Instant.now()) {
        val indices = currentTargets(spec.alias)
        check(indices.size <= 1) {
            "alias '${spec.alias}' points at ${indices.size} indices $indices; " +
                    "refusing to rebuild until it points at one"
        }
        val current = indices.singleOrNull()

        val generation = spec.generation(now)
        reap(spec, newGen = generation, now = now, live = current)

        client.indices().create {
            it.index(generation)
                .settings(parse(spec.settings, IndexSettings._DESERIALIZER))
                .mappings(parse(withMeta(spec.mappings, spec.hash), TypeMapping._DESERIALIZER))
        }

        val docs = jdbcClient.sql(spec.sql)
            .query(String::class.java)
            .list()
            .map { mapper.readTree(it) as ObjectNode }
        check(docs.isNotEmpty()) { "projection for '${spec.alias}' returned no rows; refusing to swap onto an empty index" }

        docs.chunked(BATCH).forEach { batch -> bulk(generation, batch, spec.idOf) }
        client.indices().refresh { it.index(generation) }

        swapAlias(spec, generation, current)
        log.info("swapped alias '{}' onto '{}' with {} docs", spec.alias, generation, docs.size)
    }

    private fun bulk(index: String, batch: List<ObjectNode>, idOf: (ObjectNode) -> String) {
        val request = BulkRequest.Builder()
        batch.forEach { node ->
            request.operations { op -> op.index { it.index(index).id(idOf(node)).document(node) } }
        }
        val response = client.bulk(request.build())
        check(!response.errors()) {
            "bulk into '$index' had errors: ${response.items().firstOrNull { it.error() != null }?.error()?.reason()}"
        }
    }

    private fun swapAlias(spec: IndexSpec, generation: String, current: String?) {
        client.indices().updateAliases { updateRequest ->
            current?.let { old ->
                updateRequest.actions { a -> a.remove { it.index(old).alias(spec.alias) } }
            }
            updateRequest.actions { a -> a.add { it.index(generation).alias(spec.alias) } }
        }
    }

    private fun reap(spec: IndexSpec, newGen: String, now: Instant, live: String?) {
        val stale = sameAliasIndices(spec)
            .filter { it != newGen && it != live }
            .filter { index -> spec.timestampOf(index)?.isBefore(now.minus(properties.reapMinAge)) == true }

        stale.forEach { index ->
            client.indices().delete { it.index(index) }
            log.info("reaped stale generation '{}'", index)
        }

        val staleForeign = sameBaseIndices(spec)
            .filterKeys { index -> !spec.aliasMatches(index) }
            // don't delete indices in use
            .filterValues { state -> state.aliases().isEmpty() }
            .filter { (index, _) ->
                spec.timestampOf(index)?.isBefore(now.minus(properties.foreignReapMinAge)) == true
            }

        staleForeign.keys.forEach { index ->
            client.indices().delete { it.index(index) }
            log.info("reaped stale foreign generation '{}'", index)
        }
    }

    private fun sameAliasIndices(spec: IndexSpec): Set<String> =
        client.indices().get { it.index(spec.currentAliasGlob) }.result().keys

    private fun sameBaseIndices(spec: IndexSpec): Map<String, IndexState> =
        client.indices().get { it.index("${spec.base}*") }.result()

    private fun currentTargets(alias: String): Set<String> {
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
