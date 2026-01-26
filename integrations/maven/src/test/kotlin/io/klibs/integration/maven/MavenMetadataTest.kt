package io.klibs.integration.maven

import io.klibs.integration.maven.dto.MavenMetadata
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MavenMetadataTest {

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

        val xml = XML {
            autoPolymorphic = true
            defaultPolicy {
                unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
            }
        }

        val metadata = xml.decodeFromString(MavenMetadata.serializer(), xmlText)

        assertEquals("io.klibs", metadata.groupId)
        assertEquals("core", metadata.artifactId)
        assertEquals(listOf("0.1.0", "1.0.0"), metadata.versioning.versions)
        assertEquals("1.0.0", metadata.versioning.latest)
        assertEquals("1.0.0", metadata.versioning.release)
        assertEquals("20231027123456", metadata.versioning.lastUpdated)
    }
}
