package io.klibs.integration.hub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties
import io.klibs.integration.hub.enums.HubAuthenticationFailure
import io.klibs.integration.hub.exception.HubAuthenticationException
import io.klibs.integration.hub.service.HubAuthenticationService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URLDecoder
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HubAuthenticationServiceTest {
    private val server = HubTestServer()
    private val meterRegistry = SimpleMeterRegistry()

    @AfterEach
    fun tearDown() {
        server.close()
        meterRegistry.close()
    }

    @Test
    fun `exchanges the code as a public client with PKCE then obtains the Hub subject`() {
        val uut = service()

        assertEquals("hub-user-42", uut.authenticate("code+with&symbols", VERIFIER))

        val tokenRequest = assertNotNull(server.requests.poll())
        assertEquals("POST", tokenRequest.method)
        assertEquals("/hub/api/rest/oauth2/token", tokenRequest.path)
        assertNull(tokenRequest.authorization, "A public client must not send client credentials in a header")
        assertTrue(assertNotNull(tokenRequest.contentType).startsWith("application/x-www-form-urlencoded"))
        val form = tokenRequest.body.split('&').associate {
            val (key, value) = it.split('=', limit = 2)
            URLDecoder.decode(key, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
        }
        assertEquals(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to "code+with&symbols",
                "redirect_uri" to CALLBACK_URL,
                "client_id" to "klibs-client",
                "code_verifier" to VERIFIER,
            ),
            form,
        )
        val userRequest = assertNotNull(server.requests.poll())
        assertEquals("GET", userRequest.method)
        assertEquals("/hub/api/rest/oauth2/userinfo", userRequest.path)
        assertEquals("Bearer hub-access-token", userRequest.authorization)
        assertEquals("", userRequest.body)
        assertTrue(server.requests.isEmpty())
        assertEquals(1, meterRegistry.get("klibs.auth.hub.request").tag("endpoint", "token").timer().count())
        assertEquals(1, meterRegistry.get("klibs.auth.hub.request").tag("endpoint", "userinfo").timer().count())
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401])
    fun `reports OAuth errors from the token endpoint`(status: Int) {
        server.tokenResponse = status to """{"error":"invalid_grant","error_description":"private details"}"""
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.OAUTH_ERROR, exception.failure)
        assertEquals("OAUTH_ERROR", exception.message)
        assertEquals("/hub/api/rest/oauth2/token", server.requests.single().path)
        assertEquals(1, meterRegistry.get("klibs.auth.hub.request").tag("endpoint", "token").timer().count())
        assertNull(meterRegistry.find("klibs.auth.hub.request").tag("endpoint", "userinfo").timer())
    }

    @ParameterizedTest
    @ValueSource(ints = [302, 429, 500, 503])
    fun `rejects upstream HTTP failure responses`(status: Int) {
        server.tokenResponse = status to """{"error":"upstream_failure"}"""
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.UNAVAILABLE, exception.failure)
        assertEquals(1, server.requests.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [429, 503])
    fun `reports upstream HTTP failures even when the response is not JSON`(status: Int) {
        server.tokenResponse = status to "<html>Service unavailable</html>"
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.UNAVAILABLE, exception.failure)
        assertEquals("/hub/api/rest/oauth2/token", server.requests.single().path)
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}", "{\"error\":42}", "not-json"])
    fun `rejects malformed OAuth error responses`(body: String) {
        server.tokenResponse = 400 to body
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.MALFORMED_RESPONSE, exception.failure)
        assertEquals("/hub/api/rest/oauth2/token", server.requests.single().path)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "not-json", "{}", "{\"error\":\"invalid_grant\"}", "{\"access_token\":42,\"token_type\":\"Bearer\"}",
        "{\"access_token\":\"\",\"token_type\":\"Bearer\"}",
        "{\"access_token\":\"secret\"}", "{\"access_token\":\"secret\",\"token_type\":\"MAC\"}",
        "{\"access_token\":\"secret\\r\\nInjected: value\",\"token_type\":\"Bearer\"}",
        "{\"access_token\":\"secret\",\"token_type\":\"Bearer\"} trailing-data",
    ])
    fun `rejects malformed token responses without requesting user info`(body: String) {
        server.tokenResponse = 200 to body
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.MALFORMED_RESPONSE, exception.failure)
        assertEquals("/hub/api/rest/oauth2/token", server.requests.single().path)
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-json", "{}", "{\"sub\":\"\"}", "{\"sub\":\" \"}", "{\"sub\":42}"])
    fun `rejects a missing or malformed Hub subject`(body: String) {
        server.userInfoResponse = 200 to body
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.MALFORMED_RESPONSE, exception.failure)
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403, 429, 500])
    fun `a user info failure cannot authenticate using the token response alone`(status: Int) {
        server.userInfoResponse = status to """{"error":"rejected"}"""
        val uut = service()

        val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }

        assertEquals(HubAuthenticationFailure.UNAVAILABLE, exception.failure)
        assertEquals(1, meterRegistry.get("klibs.auth.hub.request").tag("endpoint", "userinfo").timer().count())
    }

    @Test
    fun `the per-request timeout also stops a stalled token response body`() {
        server.tokenBodyGate = CountDownLatch(1)
        val uut = service(requestTimeout = Duration.ofMillis(200))

        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }
            assertEquals(HubAuthenticationFailure.TIMEOUT, exception.failure)
        }
        assertEquals("/hub/api/rest/oauth2/token", server.requests.single().path)
    }

    @Test
    fun `the per-request timeout also stops a stalled user info response body`() {
        server.userInfoBodyGate = CountDownLatch(1)
        val uut = service(requestTimeout = Duration.ofMillis(300))

        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val exception = assertFailsWith<HubAuthenticationException> { uut.authenticate("code", VERIFIER) }
            assertEquals(HubAuthenticationFailure.TIMEOUT, exception.failure)
        }
        assertEquals(
            listOf("/hub/api/rest/oauth2/token", "/hub/api/rest/oauth2/userinfo"),
            server.requests.map { it.path },
        )
        assertEquals(1, meterRegistry.get("klibs.auth.hub.request").tag("endpoint", "userinfo").timer().count())
    }

    private fun service(
        requestTimeout: Duration = Duration.ofSeconds(4),
    ) = HubAuthenticationService(
        OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(requestTimeout).build(),
        jacksonObjectMapper(),
        HubAuthenticationProperties(
            baseUrl = server.baseUrl,
            clientId = "klibs-client",
            scope = "openid",
            callbackUrl = CALLBACK_URL,
            requestTimeout = requestTimeout,
            loginAttemptTtl = Duration.ofMinutes(10),
        ),
        meterRegistry,
    )

    private companion object {
        const val CALLBACK_URL = "https://api.example/auth/hub/callback"
        const val VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    }
}
