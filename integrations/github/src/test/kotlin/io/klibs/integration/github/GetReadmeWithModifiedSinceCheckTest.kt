package io.klibs.integration.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.klibs.integration.github.model.ReadmeFetchResult
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.GitHub
import org.kohsuke.github.authorization.AuthorizationProvider
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetReadmeWithModifiedSinceCheckTest {

    private lateinit var meterRegistry: SimpleMeterRegistry

    @Mock
    private lateinit var githubApi: GitHub

    private val klibsRepoName = "JetBrains/klibs-io"

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
    }

    private fun newIntegration(
        client: OkHttpClient,
        authorizationProvider: AuthorizationProvider = AuthorizationProvider { AUTHORIZATION },
    ): GitHubIntegration =
        GitHubIntegrationKohsukeLibrary(
            meterRegistry,
            githubApi,
            githubApi,
            client,
            authorizationProvider,
            jacksonObjectMapper(),
            klibsRepoName,
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

        assertEquals(AUTHORIZATION, sentRequest.header("Authorization"))
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

    @Test
    fun `retries anonymously when organization IP allow list blocks authenticated README request`() {
        val repositoryId = 22222L
        val modifiedSince = Instant.parse("2024-06-07T08:09:10Z")
        val requests = mutableListOf<Request>()
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            requests += request
            if (request.header("Authorization") == AUTHORIZATION) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(403)
                    .message("Forbidden")
                    .body(IP_ALLOW_LIST_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            } else {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("# Anonymous README".toResponseBody("text/markdown".toMediaTypeOrNull()))
                    .build()
            }
        }
        val integration = newIntegration(OkHttpClient.Builder().addInterceptor(interceptor).build())

        val result = integration.getReadmeWithModifiedSinceCheck(repositoryId, modifiedSince)

        assertEquals("# Anonymous README", assertInstanceOf(ReadmeFetchResult.Content::class.java, result).markdown)
        assertEquals(listOf(AUTHORIZATION, null), requests.map { it.header("Authorization") })
        assertEquals(listOf("application/vnd.github.raw", "application/vnd.github.raw"), requests.map { it.header("Accept") })
        val expectedIfModifiedSince = ZonedDateTime.ofInstant(modifiedSince, ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME)
        assertEquals(listOf(expectedIfModifiedSince, expectedIfModifiedSince), requests.map { it.header("If-Modified-Since") })
    }

    @Test
    fun `does not retry unrelated forbidden README response anonymously`() {
        val authorizations = mutableListOf<String?>()
        val interceptor = Interceptor { chain ->
            authorizations += chain.request().header("Authorization")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body(UNRELATED_FORBIDDEN_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }
        val integration = newIntegration(OkHttpClient.Builder().addInterceptor(interceptor).build())

        val result = integration.getReadmeWithModifiedSinceCheck(33333L, Instant.now())

        assertEquals(403, assertInstanceOf(ReadmeFetchResult.Error::class.java, result).status)
        assertEquals(listOf(AUTHORIZATION), authorizations)
    }

    @Test
    fun `returns NotFound when anonymous README retry cannot find repository`() {
        val authorizations = mutableListOf<String?>()
        val interceptor = Interceptor { chain ->
            val authorization = chain.request().header("Authorization")
            authorizations += authorization
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(if (authorization == AUTHORIZATION) 403 else 404)
                .message(if (authorization == AUTHORIZATION) "Forbidden" else "Not Found")
                .body(
                    (if (authorization == AUTHORIZATION) IP_ALLOW_LIST_RESPONSE else "")
                        .toResponseBody("application/json".toMediaTypeOrNull())
                )
                .build()
        }
        val integration = newIntegration(OkHttpClient.Builder().addInterceptor(interceptor).build())

        val result = integration.getReadmeWithModifiedSinceCheck(44444L, Instant.now())

        assertInstanceOf(ReadmeFetchResult.NotFound::class.java, result)
        assertEquals(listOf(AUTHORIZATION, null), authorizations)
    }

    private companion object {
        const val AUTHORIZATION = "Bearer installation-token"
        const val IP_ALLOW_LIST_MESSAGE =
            "Although you appear to have the correct authorization credentials, the `JetBrains` organization has " +
                "an IP allow list enabled, and your IP address is not permitted to access this resource."
        const val IP_ALLOW_LIST_RESPONSE =
            """{"message":"$IP_ALLOW_LIST_MESSAGE","documentation_url":"https://docs.github.com/rest","status":"403"}"""
        const val UNRELATED_FORBIDDEN_RESPONSE =
            """{"message":"Resource not accessible by integration","status":"403"}"""
    }
}
