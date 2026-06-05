package io.klibs.integration.maven.search.impl

import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaseMavenSearchClientURLBuilderTest {

    private val client = object : BaseMavenSearchClient(
        xmlMapper = mock(),
        rateLimiter = mock(),
        logger = mock(),
        objectMapper = mock(),
        clientTransport = mock()
    ) {
        override fun getContentUrlPrefix(): String = "https://example.com/repo/"

        override fun searchWithThrottle(
            page: Int,
            query: Query,
            lastUpdatedSince: Instant
        ): MavenSearchResponse {
            throw UnsupportedOperationException("Not implemented")
        }
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