package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.ScraperType
import io.klibs.integration.maven.exception.MavenRateLimitedException
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import io.klibs.integration.maven.service.impl.GoogleMavenCentralMirrorStaticDataProvider
import io.klibs.integration.maven.service.impl.MAX_REDIRECTS
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.format
import org.apache.maven.search.api.transport.Transport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BaseMavenStaticDataProviderRedirectTest {

    private lateinit var transport: Transport
    private lateinit var sonatypeClient: SonatypeCentralStaticDataProvider
    private lateinit var googleMirrorClient: GoogleMavenCentralMirrorStaticDataProvider

    private val mockedNow = Instant.parse("2026-08-31T12:00:00Z")
    private val mockedClock = object : Clock {
        override fun now(): Instant = mockedNow
    }

    @BeforeEach
    fun setup() {
        transport = mock()
        sonatypeClient = createSonatypeClient(transport)
        googleMirrorClient = createGoogleMirrorClient(transport)
    }

    @Test
    fun `single redirect then ok`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val redirect = mockResponse(
            code = 301,
            headers = mapOf("location" to "https://example.com/redirected")
        )
        val ok = mockSonatypeOkResponse(
            code = 200,
            body = pom
        )
        whenever(transport.get(any(), any())).thenReturn(redirect, ok)

        val result = sonatypeClient.getPom(
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
        val ok = mockSonatypeOkResponse(code = 200, body = pom)
        whenever(transport.get(any(), any())).thenReturn(
            redirects[0],
            *redirects.subList(1, redirects.size).toTypedArray(),
            ok
        )

        val result = sonatypeClient.getPom(
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
            sonatypeClient.getPom(
                MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
            )
        }
        assertNotNull(ex.cause, "Expected underlying IOException as cause")
        assertTrue(ex.cause!!.message!!.contains("Too many redirects"))
    }

    @Test
    fun `redirect missing location header throws`() {
        val redirect = mockResponse(code = 302, headers = emptyMap())
        whenever(transport.get(any(), any())).thenReturn(redirect)

        assertFailsWith<IllegalArgumentException> {
            sonatypeClient.getPom(
                MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
            )
        }
    }

    @Test
    fun `unexpected status throws`() {
        val serverError = mockResponse(code = 500)
        whenever(transport.get(any(), any())).thenReturn(serverError)

        assertFailsWith<IllegalStateException> {
            sonatypeClient.getPom(
                MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
            )
        }
    }

    @Test
    fun `not found returns null`() {
        val notFound = mockResponse(code = 404)
        whenever(transport.get(any(), any())).thenReturn(notFound)

        val result = sonatypeClient.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )
        assertNull(result, "Expected null for HTTP 404 response")
    }

    @Test
    fun `release date uses last-modified for sonatype`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val releasedAt = "Sat, 13 Feb 2021 16:31:54 GMT"
        val ok = mockSonatypeOkResponse(
            code = 200,
            headers = mapOf("last-modified" to releasedAt),
            body = pom,
            addDefaultLastModifiedHeader = false
        )
        whenever(transport.get(any(), any())).thenReturn(ok)

        val result = sonatypeClient.getPomWithReleaseDate(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNotNull(result)
        assertEquals(Instant.parse("2021-02-13T16:31:54Z").toJavaInstant(), result.releasedAt)
    }

    @Test
    fun `release date prefers google mirror metadata over storage x-goog-meta-last-modified-epoch`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val ok = mockGoogleMirrorOkResponse(
            code = 200,
            headers = mapOf(
                "x-goog-meta-last-modified-epoch" to "1787534959000",
            ),
            body = pom,
            addDefaultLastModifiedHeader = false
        )
        whenever(transport.get(any(), any())).thenReturn(ok)

        val result = googleMirrorClient.getPomWithReleaseDate(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.GOOGLE_MAVEN_CENTRAL_MIRROR)
        )

        assertNotNull(result)
        assertEquals(Instant.parse("2026-08-24T01:29:19Z").toJavaInstant(), result.releasedAt)
    }

    @Test
    fun `pom 404 on primary falls back to upstream and returns the pom`() {
        val pom = minimalPom("org.example", "example-artifact", "1.0.0")
        val primary404 = mockResponse(code = 404)
        val upstreamOk = mockSonatypeOkResponse(code = 200, body = pom)
        whenever(transport.get(any(), any())).thenReturn(primary404, upstreamOk)

        val fallbackClient = createSonatypeClient(transport, fallbackPrefix = "https://upstream/maven2/")
        val result = fallbackClient.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNotNull(result, "Expected POM via fallback after primary 404")
        assertEquals("example-artifact", result.artifactId)
    }

    @Test
    fun `pom 404 on both primary and fallback returns null`() {
        val primary404 = mockResponse(code = 404)
        val fallback404 = mockResponse(code = 404)
        whenever(transport.get(any(), any())).thenReturn(primary404, fallback404)

        val fallbackClient = createSonatypeClient(transport, fallbackPrefix = "https://upstream/maven2/")
        val result = fallbackClient.getPom(
            MavenArtifact("org.example", "example-artifact", "1.0.0", ScraperType.CENTRAL_SONATYPE)
        )

        assertNull(result, "Expected null when fallback also returns 404")
    }

    @Test
    fun `rate limited with retry-after applies cooldown and carries the delay`() {
        val rateLimiter = passthroughRateLimiter()
        val tooMany = mockResponse(code = 429, headers = mapOf("retry-after" to "120"))
        whenever(transport.get(any(), any())).thenReturn(tooMany)

        val rateLimitedClient = createSonatypeClient(transport, rateLimiter = rateLimiter, clock = mockedClock)
        val ex = assertFailsWith<MavenRateLimitedException> {
            rateLimitedClient.getPom(
                MavenArtifact(
                    "org.example",
                    "example-artifact",
                    "1.0.0",
                    ScraperType.CENTRAL_SONATYPE
                )
            )
        }

        assertEquals(mockedNow.plus(120.seconds), ex.retryAfter)
        verify(rateLimiter).applyCooldown(eq(mockedNow.plus(120.seconds)))
    }

    @Test
    fun `rate limited with retry-after date applies cooldown and carries the instant`() {
        val rateLimiter = passthroughRateLimiter()
        val expectedRetryCooldownTime = mockedNow.plus(120.seconds)

        val retryAfterHeader = Instant.parse("2026-08-31T12:02:00Z").format(
            kotlinx.datetime.format.DateTimeComponents.Formats.RFC_1123
        )
        val tooManyRequestsResponse = mockResponse(code = 429, headers = mapOf("retry-after" to retryAfterHeader))
        whenever(transport.get(any(), any())).thenReturn(tooManyRequestsResponse)

        val uut = createSonatypeClient(transport, rateLimiter = rateLimiter, clock = mockedClock)
        val ex = assertFailsWith<MavenRateLimitedException> {
            uut.getPom(
                MavenArtifact(
                    "org.example",
                    "example-artifact",
                    "1.0.0",
                    ScraperType.CENTRAL_SONATYPE
                )
            )
        }

        assertEquals(expectedRetryCooldownTime, ex.retryAfter)
        verify(rateLimiter).applyCooldown(eq(expectedRetryCooldownTime))
    }

    @Test
    fun `rate limited without retry-after does not apply cooldown`() {
        val rateLimiter = passthroughRateLimiter()
        val tooMany = mockResponse(code = 429)
        whenever(transport.get(any(), any())).thenReturn(tooMany)

        val rateLimitedClient = createSonatypeClient(transport, rateLimiter = rateLimiter)
        val ex = assertFailsWith<MavenRateLimitedException> {
            rateLimitedClient.getPom(
                MavenArtifact(
                    "org.example",
                    "example-artifact",
                    "1.0.0",
                    ScraperType.CENTRAL_SONATYPE
                )
            )
        }

        assertNull(ex.retryAfter)
        verify(rateLimiter, never()).applyCooldown(any())
    }

    @Test
    fun `rate limited with invalid retry-after is treated as absent`() {
        val rateLimiter = passthroughRateLimiter()
        val tooMany = mockResponse(code = 429, headers = mapOf("retry-after" to "0"))
        whenever(transport.get(any(), any())).thenReturn(tooMany)

        val rateLimitedClient = createSonatypeClient(transport, rateLimiter = rateLimiter)
        val ex = assertFailsWith<MavenRateLimitedException> {
            rateLimitedClient.getPom(
                MavenArtifact(
                    "org.example",
                    "example-artifact",
                    "1.0.0",
                    ScraperType.CENTRAL_SONATYPE
                )
            )
        }

        assertNull(ex.retryAfter)
        verify(rateLimiter, never()).applyCooldown(any())
    }

    private fun passthroughRateLimiter(): MavenCentralRateLimiter {
        val rateLimiter = mock<MavenCentralRateLimiter>()
        whenever(rateLimiter.withRateLimitBlocking(any<() -> Any?>())).thenAnswer { invocation ->
            invocation.getArgument<() -> Any?>(0).invoke()
        }
        return rateLimiter
    }

    private fun createSonatypeClient(
        transport: Transport,
        fallbackPrefix: String = SONATYPE_CONTENT_PREFIX,
        rateLimiter: MavenCentralRateLimiter = passthroughRateLimiter(),
        clock: Clock = Clock.System,
    ): SonatypeCentralStaticDataProvider {
        return SonatypeCentralStaticDataProvider(
            xmlMapper = XmlMapper().apply { registerKotlinModule() },
            mavenCentralRateLimiter = rateLimiter,
            objectMapper = ObjectMapper().registerKotlinModule(),
            contentEndpoint = SONATYPE_CONTENT_PREFIX,
            contentFallbackEndpoint = fallbackPrefix,
            clientTransport = transport,
            clock = clock,
        )
    }

    private fun createGoogleMirrorClient(
        transport: Transport,
        rateLimiter: MavenCentralRateLimiter = passthroughRateLimiter(),
        clock: Clock = Clock.System,
    ): GoogleMavenCentralMirrorStaticDataProvider {
        return GoogleMavenCentralMirrorStaticDataProvider(
            xmlMapper = XmlMapper().apply { registerKotlinModule() },
            mavenCentralRateLimiter = rateLimiter,
            objectMapper = ObjectMapper().registerKotlinModule(),
            contentEndpoint = GOOGLE_MIRROR_CONTENT_PREFIX,
            contentFallbackEndpoint = GOOGLE_MIRROR_CONTENT_PREFIX,
            clientTransport = transport,
            clock = clock,
        )
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
        body: String? = null,
        addDefaultLastModifiedHeader: Boolean = false,
        defaultLastModifiedHeaderName: String? = null,
        defaultLastModifiedHeaderValue: String = DEFAULT_RFC_1123_LAST_MODIFIED,
    ): Transport.Response {
        val response = mock<Transport.Response>()
        whenever(response.code).thenReturn(code)

        val shouldAddDefaultLastModified =
            code == 200 && addDefaultLastModifiedHeader && defaultLastModifiedHeaderName != null &&
                !headers.containsKey(defaultLastModifiedHeaderName)

        val finalHeaders = if (shouldAddDefaultLastModified) {
            headers + (defaultLastModifiedHeaderName to defaultLastModifiedHeaderValue)
        } else {
            headers
        }

        whenever(response.headers).thenReturn(finalHeaders)
        val stream = ByteArrayInputStream((body ?: "").toByteArray(StandardCharsets.UTF_8))
        whenever(response.body).thenReturn(stream)
        return response
    }

    private fun mockSonatypeOkResponse(
        code: Int = 200,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        addDefaultLastModifiedHeader: Boolean = true,
    ): Transport.Response {
        return mockResponse(
            code = code,
            headers = headers,
            body = body,
            addDefaultLastModifiedHeader = addDefaultLastModifiedHeader,
            defaultLastModifiedHeaderName = "last-modified",
        )
    }

    private fun mockGoogleMirrorOkResponse(
        code: Int = 200,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        addDefaultLastModifiedHeader: Boolean = true,
    ): Transport.Response {
        return mockResponse(
            code = code,
            headers = headers,
            body = body,
            addDefaultLastModifiedHeader = addDefaultLastModifiedHeader,
            defaultLastModifiedHeaderName = "x-goog-meta-last-modified-epoch",
            defaultLastModifiedHeaderValue = DEFAULT_GOOGLE_LAST_MODIFIED_EPOCH,
        )
    }

    companion object {
        private const val SONATYPE_CONTENT_PREFIX = "https://test/remotecontent?filepath="
        private const val GOOGLE_MIRROR_CONTENT_PREFIX = "https://mirror/remotecontent?filepath="
        private const val DEFAULT_RFC_1123_LAST_MODIFIED = "Mon, 31 Aug 2026 12:00:00 GMT"
        private const val DEFAULT_GOOGLE_LAST_MODIFIED_EPOCH = "1788177600000"
    }
}
