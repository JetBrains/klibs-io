package io.klibs.integration.maven

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.klibs.integration.maven.dto.MavenMetadata
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import kotlin.test.assertEquals

class MavenMetadataTest {

    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        return builder.createXmlMapper(true).build()
    }

    @Test
    fun `test parsing maven-metadata xml`() {
        val xmlText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>io.klibs</groupId>
              <artifactId>core</artifactId>
              <versioning>
                <latest>1.0.0</latest>
                <release>1.0.0</release>
                <versions>
                  <version>0.1.0</version>
                  <version>1.0.0</version>
                </versions>
                <lastUpdated>20231027123456</lastUpdated>
              </versioning>
              <plugins>
                <plugin>
                  <name>some-plugin</name>
                </plugin>
              </plugins>
            </metadata>
        """.trimIndent()

        val xmlMapper = XmlMapper().registerKotlinModule()

        val metadata = xmlMapper.readValue(xmlText, MavenMetadata::class.java)

        assertEquals("io.klibs", metadata.groupId)
        assertEquals("core", metadata.artifactId)
        assertEquals(listOf("0.1.0", "1.0.0"), metadata.versioning.versions)
        assertEquals("1.0.0", metadata.versioning.latest)
        assertEquals("1.0.0", metadata.versioning.release)
        assertEquals("20231027123456", metadata.versioning.lastUpdated)
    }
}
