package io.klibs.core.search.opensearch

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.opensearch.client.json.JsonpDeserializer
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.IndexSettings
import java.io.StringReader

class IndexDefinitionsTest {

    private val mapper = JacksonJsonpMapper()

    private fun <T> parse(json: String, deserializer: JsonpDeserializer<T>): T =
        mapper.jsonProvider().createParser(StringReader(json)).use { deserializer.deserialize(it, mapper) }

    @Test
    fun `index definition resources are present and readable by the client mapper`() {
        assertNotNull(parse(IndexDefinitions.PROJECT_SETTINGS, IndexSettings._DESERIALIZER))
        assertNotNull(parse(IndexDefinitions.PACKAGE_SETTINGS, IndexSettings._DESERIALIZER))
        assertNotNull(parse(IndexDefinitions.PROJECT_MAPPINGS, TypeMapping._DESERIALIZER))
        assertNotNull(parse(IndexDefinitions.PACKAGE_MAPPINGS, TypeMapping._DESERIALIZER))
    }
}
