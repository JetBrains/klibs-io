package io.klibs.integration.github

import io.klibs.integration.github.model.ReadmeFetchResult
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.Interceptor
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.GitHub
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith()
class GetReadmeWithModifiedSinceCheckTest {

    private lateinit var meterRegistry: SimpleMeterRegistry

    private lateinit var githubApi: GitHub

    private lateinit var props: GitHubIntegrationProperties

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        props = GitHubIntegrationProperties(
            cache = GitHubIntegrationProperties.Cache()
        )
        githubApi = mockk()
    }

    private fun newIntegration(client: OkHttpClient): GitHubIntegration =
        GitHubIntegrationKohsukeLibrary(
            meterRegistry,
            githubApi,
            client,
            props
        )

    @Test
    fun `returns Content on 200 and sets required headers`() {
        val repositoryId = 12345L
        val modifiedSince = Instant.parse("2024-01-02T03:04:05Z")

        var capturedRequest: Request? = null
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            capturedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("# README content".toResponseBody(contentType = "text/markdown".toMediaTypeOrNull()))
                .build()
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val integration = newIntegration(client)

        val result = integration.getReadmeWithModifiedSinceCheck(repositoryId, modifiedSince)
        assert(result is ReadmeFetchResult.Content)
        assertEquals("# README content", (result as ReadmeFetchResult.Content).markdown)

        val sentRequest = requireNotNull(capturedRequest)

        val expectedIfModifiedSince = ZonedDateTime.ofInstant(modifiedSince, ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME)
        assertEquals(expectedIfModifiedSince, sentRequest.header("If-Modified-Since"))
    }

    @Test
    fun `returns NotModified on 304 Not Modified`() {
        val repositoryId = 54321L
        val modifiedSince = Instant.parse("2024-05-06T07:08:09Z")

        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(304)
                .message("Not Modified")
                .body("".toResponseBody(contentType = "text/markdown".toMediaTypeOrNull()))
                .build()
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val integration = newIntegration(client)

        val result = integration.getReadmeWithModifiedSinceCheck(repositoryId, modifiedSince)
        assert(result is ReadmeFetchResult.NotModified)
    }

    @Test
    fun `returns NotFound on 404 Not Found`() {
        val repositoryId = 11111L
        val modifiedSince = Instant.now()

        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("".toResponseBody(contentType = "text/markdown".toMediaTypeOrNull()))
                .build()
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val integration = newIntegration(client)

        val result = integration.getReadmeWithModifiedSinceCheck(repositoryId, modifiedSince)
        assert(result is ReadmeFetchResult.NotFound)
    }
}
