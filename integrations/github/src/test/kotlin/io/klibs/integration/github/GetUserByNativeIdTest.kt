package io.klibs.integration.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kohsuke.github.GitHub
import org.kohsuke.github.authorization.AuthorizationProvider
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetUserByNativeIdTest {

    private lateinit var meterRegistry: SimpleMeterRegistry

    @Mock
    private lateinit var githubApi: GitHub

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
    }

    @Test
    fun `maps the GitHub payload onto the owner, ignoring fields we do not model`() {
        val integration = newIntegration(respondWith(200, USER_PAYLOAD))

        val user = integration.getUser(62517686L)

        assertEquals(62517686L, user?.id)
        assertEquals("voize-health", user?.login)
        assertEquals("Organization", user?.type)
        assertEquals("newhandle", user?.twitterUsername)
        assertEquals(20, user?.followers)
        assertNull(user?.company, "an empty string is normalised away, not carried into the entity")
    }

    @Test
    fun `returns null on 404, the only outcome that means the account is gone`() {
        val integration = newIntegration(respondWith(404, """{"message":"Not Found"}"""))

        assertNull(integration.getUser(62517686L))
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403, 429, 500, 503])
    fun `a failure that is not a 404 is never reported as a deletion`(status: Int) {
        val integration = newIntegration(respondWith(status, """{"message":"failure"}"""))

        // returning null here would report a live owner as deleted
        val exception = assertThrows<GitHubApiException> { integration.getUser(62517686L) }

        assertEquals(status, exception.status, "the caller needs the status to tell a rate limit from a bad token")
    }

    @Test
    fun `an organization IP allow list does not hide the owner`() {
        val integration = newIntegration(
            OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val blocked = request.header("Authorization") == AUTHORIZATION
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (blocked) 403 else 200)
                    .message("message")
                    .body(
                        (if (blocked) IP_ALLOW_LIST_BODY else USER_PAYLOAD)
                            .toResponseBody(contentType = "application/json".toMediaTypeOrNull())
                    )
                    .build()
            }).build()
        )

        assertEquals("voize-health", integration.getUser(62517686L)?.login)
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
            "JetBrains/klibs-io",
        )

    private fun respondWith(code: Int, body: String) = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("message")
                .body(body.toResponseBody(contentType = "application/json".toMediaTypeOrNull()))
                .build()
        })
        .build()

    private companion object {
        private const val AUTHORIZATION = "Bearer test-token"

        private val USER_PAYLOAD = """
            {
              "id": 62517686,
              "login": "voize-health",
              "type": "Organization",
              "name": "voize",
              "company": "",
              "blog": "https://voize.de",
              "location": "Germany",
              "email": "info@voize.de",
              "bio": null,
              "twitter_username": "newhandle",
              "followers": 20,
              "an_unmapped_field": "ignored"
            }
        """.trimIndent()

        private val IP_ALLOW_LIST_BODY = """
            {"message":"Although you appear to have the correct authorization credentials, the organization has an IP allow list enabled, and your IP address is not permitted to access this resource."}
        """.trimIndent()
    }
}
