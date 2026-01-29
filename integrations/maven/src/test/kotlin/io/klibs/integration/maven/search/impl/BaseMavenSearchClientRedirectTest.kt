package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.request.impl.UnlimitedRateLimiter
import io.klibs.integration.maven.search.MavenSearchResponse
import org.apache.maven.search.api.request.Query
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        transport = mock()
        client = TestClient(transport)
    }

    @Test
    fun `single redirect then ok`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val redirect = mockResponse(
            code = 301,
            headers = mapOf("location" to "https://example.com/redirected")
        )
        val ok = mockResponse(
            code = 200,
            body = pom
        )
        whenever(transport.get(any(), any())).thenReturn(redirect, ok)

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
            mockResponse(
                code = 302,
                headers = mapOf("location" to "https://example.com/redirect/$idx")
            )
        }
        val ok = mockResponse(code = 200, body = pom)
        // enqueue 5 redirects then OK
        whenever(transport.get(any(), any())).thenReturn(redirects[0], *redirects.subList(1, redirects.size).toTypedArray(), ok)

        val result = client.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNotNull(result, "Expected non-null POM after following redirects within limit")
    }

    @Test
    fun `too many redirects throws`() {
        val redirects = (1..MAX_REDIRECTS + 1).map { idx ->
            mockResponse(
                code = if (idx % 2 == 0) 307 else 308, // mix permanent/temp redirect codes
                headers = mapOf("location" to "https://example.com/redirect/$idx")
            )
        }
        whenever(transport.get(any(), any())).thenReturn(
            redirects[0], *redirects.subList(1, redirects.size).toTypedArray()
        )

        val ex = assertFailsWith<IllegalStateException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
        assertNotNull(ex.cause, "Expected underlying IOException as cause")
        assertTrue(ex.cause!!.message!!.contains("Too many redirects"))
    }

    @Test
    fun `redirect missing location header throws`() {
        val redirect = mockResponse(code = 302, headers = emptyMap())
        whenever(transport.get(any(), any())).thenReturn(redirect)

        assertFailsWith<IllegalArgumentException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
    }

    @Test
    fun `unexpected status throws`() {
        val serverError = mockResponse(code = 500)
        whenever(transport.get(any(), any())).thenReturn(serverError)

        assertFailsWith<IllegalStateException> {
            client.getPom(MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE))
        }
    }

    @Test
    fun `not found returns null`() {
        val notFound = mockResponse(code = 404)
        whenever(transport.get(any(), any())).thenReturn(notFound)

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

    private fun mockResponse(
        code: Int,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Transport.Response {
        val response = mock<Transport.Response>()
        whenever(response.code).thenReturn(code)

        val finalHeaders = if (code == 200 && !headers.containsKey("last-modified")) {
            headers + ("last-modified" to DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
        } else {
            headers
        }

        whenever(response.headers).thenReturn(finalHeaders)
        val stream = ByteArrayInputStream((body ?: "").toByteArray(StandardCharsets.UTF_8))
        whenever(response.body).thenReturn(stream)
        return response
    }

    private class TestClient(transport: Transport) : BaseMavenSearchClient(
        xmlMapper = XmlMapper().apply { registerKotlinModule() },
        rateLimiter = UnlimitedRateLimiter(),
        logger = LoggerFactory.getLogger(TestClient::class.java),
        objectMapper = ObjectMapper(),
        clientTransport = transport
    ) {
        override fun getContentUrlPrefix(): String {
            return "https://test/remotecontent?filepath="
        }

        override fun searchWithThrottle(
            page: Int,
            query: Query,
            lastUpdatedSince: Instant
        ): MavenSearchResponse {
            throw UnsupportedOperationException("Not implemented")
        }
    }
}
