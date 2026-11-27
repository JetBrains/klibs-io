package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.UserRepository
import io.klibs.core.user.repository.UserSessionRepository
import io.klibs.core.user.service.AuthenticationHashingService
import io.klibs.integration.hub.HubTestServer
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockCookie
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestPropertySource(
    properties = [
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "klibs.auth.trusted-frontend-origin=https://frontend.example",
        "klibs.auth.session.cookie-name=test_session",
        "klibs.auth.session.cookie-secure=true",
        "klibs.auth.hub.request-timeout=1s",
    ],
)
class HubAuthenticationCallbackTest : BaseUnitWithDbLayerTest() {
    @Autowired
    private lateinit var uut: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var sessionRepository: UserSessionRepository

    @Autowired
    private lateinit var hashingService: AuthenticationHashingService

    @BeforeEach
    fun setUp() {
        server.reset()
    }

    @Test
    fun `a successful callback creates a usable local session and consumes the login cookie`() {
        val login = startLogin("/search?q=C%2B%2B")

        val response = callback(login)

        assertEquals(302, response.status)
        assertEquals("https://frontend.example/search?q=C%2B%2B", response.redirectedUrl)
        assertLoginCookieDeleted(response)
        val cookie = assertNotNull(response.getCookie("test_session"))
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.secure)
        assertEquals("Lax", cookie.getAttribute("SameSite"))
        assertTrue(cookie.maxAge > 0)
        assertEquals("/", cookie.path)
        assertEquals(2, response.getHeaders(HttpHeaders.SET_COOKIE).size)
        val user = userRepository.findAll().single()
        assertEquals(hashingService.hashExternalIdentity(ExternalUserIdentity("hub-user-42")), user.externalUserIdHash)
        val session = sessionRepository.findAll().single()
        assertEquals(hashingService.hashSessionToken(cookie.value), session.tokenHash)
        assertFalse(cookie.value.contains("hub-access-token"))
        uut.get("/auth/current-user") { cookie(cookie) }.andExpect {
            status { isOk() }
            jsonPath("$.userId") { value(user.id.toString()) }
        }
    }

    @Test
    fun `a later login for the same Hub subject reuses the local user and issues a new session`() {
        val first = callback(startLogin()).getCookie("test_session")
        val userId = userRepository.findAll().single().id

        val second = callback(startLogin()).getCookie("test_session")

        assertNotEquals(assertNotNull(first).value, assertNotNull(second).value)
        assertEquals(userId, userRepository.findAll().single().id)
        assertEquals(2, sessionRepository.count())
    }

    @Test
    fun `a callback without a browser login cookie cannot contact Hub or create a session`() {
        val response = uut.get("/auth/hub/callback") {
            param("state", "unsolicited-state")
            param("code", "authorization-code")
        }.andReturn().response

        assertRejectedWithoutPersistence(response, 400)
        assertTrue(response.getHeaders(HttpHeaders.SET_COOKIE).isEmpty())
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `a callback without state leaves the current browser login attempt usable`() {
        val login = startLogin()
        val response = uut.get("/auth/hub/callback") {
            cookie(loginCookie(login))
            param("code", "authorization-code")
        }.andReturn().response

        assertRejectedWithoutPersistence(response, 400)
        assertTrue(response.getHeaders(HttpHeaders.SET_COOKIE).isEmpty())
        assertTrue(server.requests.isEmpty())
        assertEquals(302, callback(login).status)
    }

    @Test
    fun `an unmatched state does not clear the current browser login attempt`() {
        val login = startLogin()
        val response = uut.get("/auth/hub/callback") {
            cookie(loginCookie(login))
            param("state", "another-state")
            param("code", "authorization-code")
        }.andReturn().response

        assertRejectedWithoutPersistence(response, 400)
        assertTrue(response.getHeaders(HttpHeaders.SET_COOKIE).isEmpty())
        assertTrue(server.requests.isEmpty())
        assertEquals(302, callback(login).status)
    }

    @Test
    fun `a matching but tampered state clears its cookie without contacting Hub`() {
        val login = startLogin()
        val state = state(login)
        val tampered = state.dropLast(1) + if (state.last() == '0') '1' else '0'
        val cookie = loginCookie(login)
        cookie.value = cookie.value.replace(state, tampered)
        val response = uut.get("/auth/hub/callback") {
            cookie(cookie)
            param("state", tampered)
            param("code", "authorization-code")
        }.andReturn().response

        assertRejectedWithoutPersistence(response, 400)
        assertLoginCookieDeleted(response)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `a matching expired attempt clears its cookie without contacting Hub`() {
        val login = startLogin()
        val originalState = state(login)
        val payload = "0.${originalState.substringAfter('.').substringBeforeLast('.')}"
        val expiredState = "$payload.${hashingService.signOAuthState(payload)}"
        val cookie = loginCookie(login)
        cookie.value = cookie.value.replace(originalState, expiredState)

        val response = uut.get("/auth/hub/callback") {
            cookie(cookie)
            param("state", expiredState)
            param("code", "authorization-code")
        }.andReturn().response

        assertRejectedWithoutPersistence(response, 400)
        assertLoginCookieDeleted(response)
        assertTrue(server.requests.isEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `a matching callback without a usable code consumes the attempt without contacting Hub`(code: String?) {
        val response = callback(startLogin(), code = code)

        assertRejectedWithoutPersistence(response, 400)
        assertLoginCookieDeleted(response)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `an OAuth denial consumes the matching attempt even if a code is also supplied`() {
        val response = callback(startLogin(), error = "access_denied")

        assertRejectedWithoutPersistence(response, 400)
        assertLoginCookieDeleted(response)
        assertTrue(server.requests.isEmpty())
    }

    @ParameterizedTest
    @CsvSource("400,400", "401,400", "500,502", "503,502")
    fun `token endpoint failures clear the attempt without creating local credentials`(upstream: Int, expected: Int) {
        server.tokenResponse = upstream to """{"error":"invalid_grant","error_description":"private details"}"""

        val response = callback(startLogin())

        assertRejectedWithoutPersistence(response, expected, "token endpoint HTTP $upstream")
        assertLoginCookieDeleted(response)
        assertEquals(1, server.requests.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [302, 307])
    fun `the configured Hub client never follows a token endpoint redirect`(status: Int) {
        server.tokenResponse = status to """{"error":"unexpected_redirect"}"""

        val response = callback(startLogin())

        assertRejectedWithoutPersistence(response, 502, "token endpoint HTTP $status redirect")
        assertLoginCookieDeleted(response)
        assertEquals(
            listOf("/hub/api/rest/oauth2/token"),
            server.requests.map { it.path },
            "HTTP $status must not send credentials or a code to the controlled redirect target",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-json", "{}", "{\"sub\":\"\"}"])
    fun `a malformed Hub user response cannot create a local user or session`(body: String) {
        server.userInfoResponse = 200 to body

        val response = callback(startLogin())

        assertRejectedWithoutPersistence(response, 502, "userinfo response=$body")
        assertLoginCookieDeleted(response)
    }

    @Test
    fun `a stalled Hub response returns 504 and clears the matching login cookie`() {
        server.userInfoBodyGate = CountDownLatch(1)
        try {
            val response = callback(startLogin())

            assertRejectedWithoutPersistence(response, 504)
            assertLoginCookieDeleted(response)
        } finally {
            server.userInfoBodyGate?.countDown()
        }
    }

    @Test
    fun `a replay after the browser has consumed the login cookie does not create another session`() {
        val login = startLogin()
        assertEquals(302, callback(login).status)
        server.requests.clear()

        val response = uut.get("/auth/hub/callback") {
            param("state", state(login))
            param("code", "authorization-code")
        }.andReturn().response

        assertEquals(400, response.status)
        assertEquals(1, userRepository.count())
        assertEquals(1, sessionRepository.count())
        assertTrue(server.requests.isEmpty())
    }

    private fun startLogin(returnTo: String = "/"): MockHttpServletResponse =
        uut.get("/auth/hub/sign-in") { param("returnTo", returnTo) }
            .andExpect { status { isFound() } }.andReturn().response

    private fun loginCookie(response: MockHttpServletResponse): Cookie =
        MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE)))

    private fun state(response: MockHttpServletResponse): String = URLDecoder.decode(
        UriComponentsBuilder.fromUriString(assertNotNull(response.redirectedUrl)).build()
            .queryParams.getFirst("state"),
        Charsets.UTF_8,
    )

    private fun callback(
        login: MockHttpServletResponse,
        code: String? = "authorization-code",
        error: String? = null,
    ): MockHttpServletResponse = uut.get("/auth/hub/callback") {
        cookie(loginCookie(login))
        param("state", state(login))
        if (code != null) param("code", code)
        if (error != null) param("error", error)
    }.andReturn().response

    private fun assertRejectedWithoutPersistence(
        response: MockHttpServletResponse,
        status: Int,
        scenario: String = "callback validation",
    ) {
        assertEquals(status, response.status, "$scenario: expected an HTTP error")
        assertEquals("", response.contentAsString, "$scenario: error body must be empty")
        assertEquals(null, response.redirectedUrl, "$scenario: must not redirect")
        val cacheControl = assertNotNull(
            response.getHeader(HttpHeaders.CACHE_CONTROL), "$scenario: missing cache policy",
        )
        assertTrue(cacheControl.contains("no-store"), "$scenario: expected no-store, got $cacheControl")
        assertEquals(0, userRepository.count(), "$scenario: must not create a user")
        assertEquals(0, sessionRepository.count(), "$scenario: must not create a session")
        assertTrue(response.cookies.none { it.name == "test_session" }, "$scenario: must not set a session cookie")
    }

    private fun assertLoginCookieDeleted(response: MockHttpServletResponse) {
        val cookie = assertNotNull(response.getCookie("test_session_login"))
        assertEquals(0, cookie.maxAge)
        assertEquals("", cookie.value)
    }

    companion object {
        private val server = HubTestServer()

        @JvmStatic
        @DynamicPropertySource
        fun hubProperties(registry: DynamicPropertyRegistry) {
            registry.add("klibs.auth.hub.base-url") { server.baseUrl }
        }

        @JvmStatic
        @AfterAll
        fun stopHub() {
            server.close()
        }
    }
}
