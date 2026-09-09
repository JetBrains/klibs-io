package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.service.impl.GoogleMavenCentralMirrorStaticDataProvider
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GoogleMavenCentralMirrorStaticDataProviderTest {

    @Test
    fun `epoch header remains supported`() {
        val releasedAt = fetchReleaseDate(mapOf("x-goog-meta-last-modified-epoch" to "1788177600000"))

        assertEquals(Instant.parse("2026-08-31T12:00:00Z"), releasedAt)
    }

    @Test
    fun `mtime is used when epoch header is absent`() {
        val releasedAt = fetchReleaseDate(mapOf("x-goog-meta-mtime" to "2018-10-02T15:39:33.000000000Z"))

        assertEquals(Instant.parse("2018-10-02T15:39:33Z"), releasedAt)
    }

    @Test
    fun `epoch header takes precedence over mtime`() {
        val releasedAt = fetchReleaseDate(
            mapOf(
                "x-goog-meta-last-modified-epoch" to "1788177600000",
                "x-goog-meta-mtime" to "2018-10-02T15:39:33.000000000Z",
            )
        )

        assertEquals(Instant.parse("2026-08-31T12:00:00Z"), releasedAt)
    }

    @Test
    fun `release date headers are case insensitive`() {
        assertEquals(
            Instant.parse("2026-08-31T12:00:00Z"),
            fetchReleaseDate(mapOf("X-Goog-Meta-Last-Modified-Epoch" to "1788177600000")),
        )
        assertEquals(
            Instant.parse("2018-10-02T15:39:33.123456789Z"),
            fetchReleaseDate(mapOf("X-Goog-Meta-Mtime" to "2018-10-02T15:39:33.123456789Z")),
        )
    }

    @Test
    fun `missing metadata headers fail even when Last-Modified is present`() {
        for (headers in listOf(emptyMap(), mapOf("Last-Modified" to "Mon, 22 Jul 2019 22:43:40 GMT"))) {
            val exception = assertFailsWith<IllegalStateException> { fetchReleaseDate(headers) }

            assertTrue(exception.message.orEmpty().contains("Missing release date header"))
        }
    }

    @Test
    fun `invalid mtime fails as an invalid release date`() {
        val exception = assertFailsWith<IllegalStateException> {
            fetchReleaseDate(mapOf("x-goog-meta-mtime" to "not-a-date"))
        }

        assertTrue(exception.message.orEmpty().contains("Invalid release date format"))
        assertNotNull(exception.cause)
    }

    @Test
    fun `invalid epoch does not silently fall back to mtime`() {
        val exception = assertFailsWith<IllegalStateException> {
            fetchReleaseDate(
                mapOf(
                    "x-goog-meta-last-modified-epoch" to "not-an-epoch",
                    "x-goog-meta-mtime" to "2018-10-02T15:39:33Z",
                )
            )
        }

        assertTrue(exception.message.orEmpty().contains("Invalid release date format"))
        assertNotNull(exception.cause)
    }

    private fun fetchReleaseDate(headers: Map<String, String>): Instant {
        val response = mock<Transport.Response>()
        whenever(response.code).thenReturn(200)
        whenever(response.headers).thenReturn(headers)
        whenever(response.body).thenReturn(
            """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jetbrains.kotlinx</groupId>
                    <artifactId>kotlinx-coroutines-core</artifactId>
                    <version>0.30.1</version>
                </project>
            """.trimIndent().byteInputStream()
        )
        val transport = mock<Transport>()
        whenever(transport.get(any(), any())).thenReturn(response)
        val rateLimiter = mock<MavenCentralRateLimiter>()
        whenever(rateLimiter.withRateLimitBlocking(any<() -> Any?>())).thenAnswer { invocation ->
            invocation.getArgument<() -> Any?>(0).invoke()
        }
        val uut = GoogleMavenCentralMirrorStaticDataProvider(
            xmlMapper = XmlMapper(),
            mavenCentralRateLimiter = rateLimiter,
            objectMapper = ObjectMapper(),
            contentEndpoint = "https://mirror.example/maven2/",
            clientTransport = transport,
        )
        val artifact = MavenArtifact(
            "org.jetbrains.kotlinx", "kotlinx-coroutines-core", "0.30.1", ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR
        )
        val (_, releasedAt) = assertNotNull(uut.getPomWithReleaseDate(artifact))
        return releasedAt
    }
}