package io.klibs.core.search.opensearch

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.opensearch.client.json.JsonpDeserializer
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.IndexSettings
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.StringReader

/**
 * Creates the project + package OpenSearch indices with their mappings on startup if absent.
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchIndexBootstrap(
    private val client: OpenSearchClient,
    private val properties: OpenSearchProperties,
    private val mapper: JacksonJsonpMapper,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun bootstrap() {
        createIfAbsent(properties.projectIndex, IndexDefinitions.PROJECT_SETTINGS, IndexDefinitions.PROJECT_MAPPINGS)
        createIfAbsent(properties.packageIndex, IndexDefinitions.PACKAGE_SETTINGS, IndexDefinitions.PACKAGE_MAPPINGS)
    }

    private fun createIfAbsent(index: String, settingsJson: String, mappingsJson: String) {
        if (client.indices().exists { it.index(index) }.value()) {
            log.info("OpenSearch index '{}' already exists", index)
            return
        }
        val settings = parse(settingsJson, IndexSettings._DESERIALIZER)
        val mappings = parse(mappingsJson, TypeMapping._DESERIALIZER)
        client.indices().create { it.index(index).settings(settings).mappings(mappings) }
        log.info("Created OpenSearch index '{}'", index)
    }

    private fun <T> parse(json: String, deserializer: JsonpDeserializer<T>): T =
        mapper.jsonProvider().createParser(StringReader(json)).use { deserializer.deserialize(it, mapper) }

    private companion object {
        private val log = LoggerFactory.getLogger(OpenSearchIndexBootstrap::class.java)
    }
}
