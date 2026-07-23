package io.klibs.integration.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.klibs.integration.github.configuration.GitHubIntegrationConfiguration
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.security.KeyPairGenerator
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.kohsuke.github.HttpException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(
    classes = [
        GitHubIntegrationConfiguration::class,
        GitHubIntegrationKohsukeLibraryTest.SpringTestConfiguration::class,
    ]
)
class GitHubIntegrationKohsukeLibraryTest {

    @Autowired
    private lateinit var uut: GitHubIntegrationKohsukeLibrary

    @Autowired
    private lateinit var requestRecorder: RequestRecorder

    @BeforeTest
    fun resetRequestRecorder() {
        requestRecorder.authorizations.clear()
        requestRecorder.authenticatedError = IP_ALLOW_LIST_MESSAGE
        requestRecorder.anonymousNotFound = false
    }

    // Some organizations use a whitelist of IP's restrict authorised access to their public resources.
    // At the same time, anonymous access is allowed. That is why this test exists.
    @Test
    fun `retries GitHub App user lookup anonymously when organization blocks its IP`() {
        val user = uut.getUser("JetBrains")

        assertEquals("JetBrains", user?.login)
        assertEquals(listOf(AUTHORIZATION, null), requestRecorder.authorizations)
    }

    @Test
    fun `does not retry unrelated forbidden response anonymously`() {
        requestRecorder.authenticatedError = "Resource not accessible by integration"

        val exception = assertFailsWith<HttpException> {
            uut.getUser("JetBrains")
        }

        assertEquals(403, exception.responseCode)
        assertEquals(listOf<String?>(AUTHORIZATION), requestRecorder.authorizations)
    }

    @Test
    fun `returns null when anonymous retry cannot find user`() {
        requestRecorder.anonymousNotFound = true

        val user = uut.getUser("JetBrains")

        assertNull(user)
        assertEquals(listOf(AUTHORIZATION, null), requestRecorder.authorizations)
    }

    @TestConfiguration
    class SpringTestConfiguration {

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper()

        @Bean
        fun requestRecorder(): RequestRecorder = RequestRecorder()

        @Bean
        @Primary
        fun testOkHttpClient(requestRecorder: RequestRecorder): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val path = chain.request().url.encodedPath
                val authorization = chain.request().header("Authorization")
                val (code, message, body) = when (path) {
                    "/app" -> Triple(200, "OK", APP_RESPONSE)
                    "/app/installations/$INSTALLATION_ID" -> Triple(200, "OK", INSTALLATION_RESPONSE)
                    "/app/installations/$INSTALLATION_ID/access_tokens" -> Triple(201, "Created", TOKEN_RESPONSE)
                    "/rate_limit" -> Triple(200, "OK", RATE_LIMIT_RESPONSE)
                    "/users/JetBrains" -> {
                        requestRecorder.authorizations += authorization
                        if (authorization == AUTHORIZATION) {
                            Triple(403, "Forbidden", errorResponse(requestRecorder.authenticatedError))
                        } else if (requestRecorder.anonymousNotFound) {
                            Triple(404, "Not Found", NOT_FOUND_RESPONSE)
                        } else {
                            Triple(200, "OK", USER_RESPONSE)
                        }
                    }
                    else -> error("Unexpected GitHub request: ${chain.request().method} $path")
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(message)
                    .body(body.toResponseBody(JSON_MEDIA_TYPE))
                    .build()
            }
            .build()
    }

    class RequestRecorder {
        val authorizations = mutableListOf<String?>()
        var authenticatedError: String = IP_ALLOW_LIST_MESSAGE
        var anonymousNotFound: Boolean = false
    }

    private companion object {
        const val INSTALLATION_ID = 42L
        const val AUTHORIZATION = "token installation-token"
        const val IP_ALLOW_LIST_MESSAGE =
            "Although you appear to have the correct authorization credentials, the `JetBrains` organization has " +
                "an IP allow list enabled, and your IP address is not permitted to access this resource."
        const val NOT_FOUND_RESPONSE =
            """{"message":"Not Found","documentation_url":"https://docs.github.com/rest","status":"404"}"""
        const val USER_RESPONSE =
            """{"id":12345678,"login":"JetBrains","type":"Organization","name":"JetBrains"}"""
        const val APP_RESPONSE =
            """{"id":1,"slug":"test-app","name":"Test App","owner":{"id":1,"login":"test","type":"Organization"}}"""
        const val INSTALLATION_RESPONSE =
            """{"id":$INSTALLATION_ID,"app_id":1,"target_id":1,"target_type":"Organization","permissions":{},"events":[],"repository_selection":"all"}"""
        const val TOKEN_RESPONSE =
            """{"token":"installation-token","expires_at":"2099-01-01T00:00:00Z","permissions":{},"repository_selection":"all"}"""
        const val RATE_LIMIT_RESPONSE =
            """{"resources":{"core":{"limit":5000,"remaining":5000,"reset":4102444800},"search":{"limit":30,"remaining":30,"reset":4102444800},"graphql":{"limit":5000,"remaining":5000,"reset":4102444800},"integration_manifest":{"limit":5000,"remaining":5000,"reset":4102444800}},"rate":{"limit":5000,"remaining":5000,"reset":4102444800}}"""
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private fun errorResponse(message: String): String =
            """{"message":"$message","documentation_url":"https://docs.github.com/rest","status":"403"}"""

        @JvmStatic
        @DynamicPropertySource
        fun githubProperties(registry: DynamicPropertyRegistry) {
            registry.add("klibs.integration.github.app.client-id") { "client-id" }
            registry.add("klibs.integration.github.app.installation-id") { INSTALLATION_ID }
            registry.add("klibs.integration.github.app.private-key") { createPrivateKey() }
            registry.add("klibs.integration.github.cache.request-cache-size-mb") { 10 }
            registry.add("klibs.integration.github.webhook.secret") { "test-secret" }
            registry.add("klibs.integration.github.index-requests.repository") { "JetBrains/klibs-io" }
        }

        private fun createPrivateKey(): String {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val encoded = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded)
            return "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
        }
    }
}