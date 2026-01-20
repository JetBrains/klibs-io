package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.UnlimitedRateLimiter
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class BaseMavenSearchClientRedirectTest {

    private lateinit var transport: Transport
    private lateinit var client: TestClient

    @BeforeEach
    fun setup() {
        // will be initialized per test with appropriate response sequence
        transport = SequenceTransport(ArrayDeque())
        client = TestClient(transport)
    }

    @Test
    fun `single redirect then ok`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val redirect = fakeResponse(
            code = 301,
            headers = mapOf("location" to "https://example.com/redirected")
        )
        val ok = fakeResponse(
            code = 200,
            body = pom
        )
        transport = SequenceTransport(ArrayDeque(listOf(redirect, ok)))
        client = TestClient(transport)

        val result = client.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNotNull(result, "Expected non-null POM after following redirect")
        assertEquals("example-artifact", result.artifactId)
        assertEquals("org.example", result.groupId)
        assertEquals("1.0.0", result.version)
    }

    @Test
    fun `multiple redirects within limit then ok`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val redirects = (1..MAX_REDIRECTS).map { idx ->
            fakeResponse(
                code = 302,
                headers = mapOf("location" to "https://example.com/redirect/$idx")
            )
        }
        val ok = fakeResponse(code = 200, body = pom)
        // enqueue 5 redirects then OK
        transport = SequenceTransport(ArrayDeque(redirects + ok))
        client = TestClient(transport)

        val result = client.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNotNull(result, "Expected non-null POM after following redirects within limit")
    }

    @Test
    fun `too many redirects throws`() {
        val redirects = (1..MAX_REDIRECTS + 1).map { idx ->
            fakeResponse(
                code = if (idx % 2 == 0) 307 else 308, // mix permanent/temp redirect codes
                headers = mapOf("location" to "https://example.com/redirect/$idx")
            )
        }
        transport = SequenceTransport(ArrayDeque(redirects))
        client = TestClient(transport)

        val ex = assertFailsWith<IllegalStateException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
        assertNotNull(ex.cause, "Expected underlying IOException as cause")
        assertTrue(ex.cause!!.message!!.contains("Too many redirects"))
    }

    @Test
    fun `redirect missing location header throws`() {
        val redirect = fakeResponse(code = 302, headers = emptyMap())
        transport = SequenceTransport(ArrayDeque(listOf(redirect)))
        client = TestClient(transport)

        assertFailsWith<IllegalArgumentException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
    }

    @Test
    fun `unexpected status throws`() {
        val serverError = fakeResponse(code = 500)
        transport = SequenceTransport(ArrayDeque(listOf(serverError)))
        client = TestClient(transport)

        assertFailsWith<IllegalStateException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
    }

    @Test
    fun `not found returns null`() {
        val notFound = fakeResponse(code = 404)
        transport = SequenceTransport(ArrayDeque(listOf(notFound)))
        client = TestClient(transport)

        val result = client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        assertNull(result, "Expected null for HTTP 404 response")
    }

    private fun minimalPom(groupId: String, artifactId: String, version: String): String = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<project xmlns="http://maven.apache.org/POM/4.0.0"
        |         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
        |  <modelVersion>4.0.0</modelVersion>
        |  <groupId>$groupId</groupId>
        |  <artifactId>$artifactId</artifactId>
        |  <version>$version</version>
        |</project>
    """.trimMargin()

    private fun fakeResponse(
        code: Int,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Transport.Response {
        val stream: InputStream = ByteArrayInputStream((body ?: "").toByteArray(StandardCharsets.UTF_8))
        return object : Transport.Response {
            override val code: Int = code
            override val headers: Map<String, String> = headers
            override val body: InputStream = stream
        }
    }

    private class TestClient(transport: Transport) : BaseMavenSearchClient(
        baseUrl = "https://test",
        rateLimiter = UnlimitedRateLimiter(),
        logger = LoggerFactory.getLogger(TestClient::class.java),
        objectMapper = ObjectMapper(),
        clientTransport = transport
    ) {
        override fun getContentUrlPrefix(): String {
            return "https://test/remotecontent?filepath="
        }
    }

    private class SequenceTransport(private val responses: ArrayDeque<Transport.Response>) : Transport {
        override fun get(url: String, headers: Map<String, String>): Transport.Response {
            if (responses.isEmpty()) throw IllegalStateException("No more responses queued")
            return responses.removeFirst()
        }

        override fun head(url: String, headers: Map<String, String>): Transport.Response {
            if (responses.isEmpty()) throw IllegalStateException("No more responses queued")
            return responses.first() // do not consume for HEAD
        }
    }
}
