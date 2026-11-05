package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SonatypeCentralStaticDataProviderTest {

    @ParameterizedTest
    @ValueSource(strings = ["", " \t\r\n", "\"\"", " \n\"\"\t"])
    fun `empty tooling metadata is missing`(body: String) {
        assertNull(provider(body).getKotlinToolingMetadata(artifact))
    }

    @ParameterizedTest
    @ValueSource(strings = ["\"not metadata\"", "\" \"", "{}", "{", "null", "[]", "\"\" trailing"])
    fun `non-empty invalid tooling metadata fails`(body: String) {
        assertFailsWith<IllegalArgumentException> {
            provider(body).getKotlinToolingMetadata(artifact)
        }
    }

    @Test
    fun `valid tooling metadata is parsed`() {
        val body = """
            {
              "schemaVersion": "1.1.0",
              "buildSystem": "Gradle",
              "buildSystemVersion": "8.0",
              "buildPlugin": "org.jetbrains.kotlin.multiplatform",
              "buildPluginVersion": "2.0.0",
              "projectSettings": {
                "isHmppEnabled": true,
                "isCompatibilityMetadataVariantEnabled": false,
                "isKPMEnabled": false
              },
              "projectTargets": []
            }
        """.trimIndent()

        val metadata = assertNotNull(provider(body).getKotlinToolingMetadata(artifact))

        assertEquals("1.1.0", metadata.schemaVersion)
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals("2.0.0", metadata.kotlinVersion)
        assertEquals(emptyList(), metadata.projectTargets)
    }

    private fun provider(body: String): SonatypeCentralStaticDataProvider {
        val response = mockk<Transport.Response>(relaxUnitFun = true)
        every { response.code } returns 200
        every { response.body } returns body.byteInputStream()
        val transport = mockk<Transport>()
        every { transport.get(any(), any()) } returns response
        val rateLimiter = mockk<MavenCentralRateLimiter>()
        every { rateLimiter.withRateLimitBlocking(any<() -> Any?>()) } answers { firstArg<() -> Any?>().invoke() }
        return SonatypeCentralStaticDataProvider(
            xmlMapper = XmlMapper(),
            mavenCentralRateLimiter = rateLimiter,
            objectMapper = ObjectMapper(),
            contentEndpoint = "https://test/",
            clientTransport = transport,
        )
    }

    private val artifact = MavenArtifact("org.example", "library", "1.0.0", ScraperType.CENTRAL_SONATYPE)
}