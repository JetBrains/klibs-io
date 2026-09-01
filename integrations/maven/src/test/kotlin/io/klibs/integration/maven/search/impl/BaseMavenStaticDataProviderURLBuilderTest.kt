package io.klibs.integration.maven.search.impl

import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.service.impl.BaseMavenStaticDataProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BaseMavenStaticDataProviderURLBuilderTest {

    private val client = object : BaseMavenStaticDataProvider(
        xmlMapper = mock(),
        rateLimiter = mock(),
        logger = mock(),
        objectMapper = mock(),
        clientTransport = mock(),
        lastModifiedHeader = "last-modified"
    ) {
        override fun getContentUrlPrefix(): String = "https://example.com/repo/"
        override fun parseReleasedAt(value: String): Instant {
            TODO("Not yet implemented")
        }

        override val scraperType: ScraperType
            get() = TODO("Not yet implemented")

    }

    @Test
    fun `getRemoteFileUrl builds correct URL when fileName starts with dot`() {
        val url = client.getRemoteFileUrl(
            groupId = "org.jetbrains.kotlin",
            artifactId = "kotlin-stdlib",
            version = "1.9.0",
            fileName = ".pom"
        )

        assertEquals(
            "https://example.com/repo/org/jetbrains/kotlin/kotlin-stdlib/1.9.0/kotlin-stdlib-1.9.0.pom",
            url
        )
    }

    @Test
    fun `getRemoteFileUrl builds correct URL when fileName starts with dash`() {
        val url = client.getRemoteFileUrl(
            groupId = "org.jetbrains.kotlin",
            artifactId = "kotlin-stdlib",
            version = "1.9.0",
            fileName = "-kotlin-tooling-metadata.json"
        )

        assertEquals(
            "https://example.com/repo/org/jetbrains/kotlin/kotlin-stdlib/1.9.0/kotlin-stdlib-1.9.0-kotlin-tooling-metadata.json",
            url
        )
    }

    @Test
    fun `getRemoteFileUrl builds correct URL when version has characters to be encoded`() {
        val url = client.getRemoteFileUrl(
            groupId = "org.jetbrains.kotlin",
            artifactId = "kotlin-stdlib",
            version = "$1.9.0",
            fileName = "-kotlin-tooling-metadata.json"
        )

        assertEquals(
            "https://example.com/repo/org/jetbrains/kotlin/kotlin-stdlib/%241.9.0/kotlin-stdlib-%241.9.0-kotlin-tooling-metadata.json",
            url
        )
    }

    @Test
    fun `getRemoteFileUrl throws exception when fileName does not start with dash or dot`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            client.getRemoteFileUrl(
                groupId = "org.jetbrains.kotlin",
                artifactId = "kotlin-stdlib",
                version = "1.9.0",
                fileName = "invalid.pom"
            )
        }

        assertEquals("fileName must begin with - or .", exception.message)
    }
}