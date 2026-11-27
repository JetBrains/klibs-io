package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import io.klibs.app.service.LoginCookieService
import io.klibs.core.user.dto.OAuthLoginCallbackDto
import io.klibs.core.user.service.PkceService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockCookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLDecoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestPropertySource(
    properties = [
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "klibs.auth.trusted-frontend-origin=https://frontend.example",
        "klibs.auth.session.cookie-name=__Host-test_session",
        "klibs.auth.session.cookie-secure=true",
        "klibs.auth.hub.base-url=https://hub.example/hub/",
        "klibs.auth.hub.client-id=test+klibs&client",
        "klibs.auth.hub.scope=openid profile",
        "klibs.auth.hub.login-attempt-ttl=7m",
    ],
)
class HubAuthenticationControllerTest : BaseUnitWithDbLayerTest() {
    @Autowired
    private lateinit var uut: MockMvc

    @Autowired
    private lateinit var loginCookieService: LoginCookieService

    @Autowired
    private lateinit var pkceService: PkceService

    @Test
    fun `sign-in redirects to Hub with PKCE and a matching browser login cookie`() {
        val result = uut.get("/auth/hub/sign-in") {
            param("returnTo", "/search?q=C%2B%2B")
        }.andExpect {
            status { isFound() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            content { string("") }
        }.andReturn()
        val response = result.response
        val location = URI(assertNotNull(response.redirectedUrl))
        assertEquals("https", location.scheme)
        assertEquals("hub.example", location.host)
        assertEquals("/hub/api/rest/oauth2/auth", location.path)
        val callback = validateLoginAttempt(response)
        assertEquals("https://frontend.example/search?q=C%2B%2B", callback.redirectUrl)
        val parameters = authorizationParameters(response)
        assertEquals(
            mapOf(
                "response_type" to "code",
                "client_id" to "test+klibs&client",
                "redirect_uri" to "https://api.example/auth/hub/callback",
                "scope" to "openid profile",
                "state" to assertNotNull(parameters["state"]),
                "request_credentials" to "default",
                "code_challenge" to pkceService.createChallenge(callback.codeVerifier),
                "code_challenge_method" to "S256",
            ),
            parameters,
        )
        assertFalse(location.toString().contains(callback.codeVerifier))
        assertNull(result.request.getSession(false))
        assertLoginCookie(response)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["https://attacker.example", "//attacker.example", "/%5cattacker.example", "/%0a"])
    fun `sign-in falls back to the frontend root for a missing or unsafe return path`(returnTo: String?) {
        val response = uut.get("/auth/hub/sign-in") {
            if (returnTo != null) param("returnTo", returnTo)
        }.andExpect {
            status { isFound() }
        }.andReturn().response

        assertEquals("https://frontend.example/", validateLoginAttempt(response).redirectUrl, "returnTo=$returnTo")
    }

    @Test
    fun `starting sign-in again replaces the browser login attempt`() {
        val first = uut.get("/auth/hub/sign-in").andReturn().response
        val previousCookie = MockCookie.parse(assertNotNull(first.getHeader(HttpHeaders.SET_COOKIE)))

        val second = uut.get("/auth/hub/sign-in") {
            cookie(previousCookie)
        }.andExpect {
            status { isFound() }
        }.andReturn().response

        val firstParameters = authorizationParameters(first)
        val secondParameters = authorizationParameters(second)
        assertNotEquals(firstParameters["state"], secondParameters["state"])
        assertNotEquals(firstParameters["code_challenge"], secondParameters["code_challenge"])
        assertNotEquals(previousCookie.value, second.getCookie(previousCookie.name)?.value)
        assertEquals("https://frontend.example/", validateLoginAttempt(second).redirectUrl)
    }

    private fun authorizationParameters(response: MockHttpServletResponse): Map<String, String> =
        UriComponentsBuilder.fromUriString(assertNotNull(response.redirectedUrl)).build()
            .queryParams.toSingleValueMap()
            .mapValues { (_, value) -> URLDecoder.decode(value, Charsets.UTF_8) }

    private fun validateLoginAttempt(response: MockHttpServletResponse): OAuthLoginCallbackDto {
        val cookie = MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE)))
        val request = MockHttpServletRequest().apply { setCookies(cookie) }
        return assertNotNull(
            loginCookieService.validateAndConsumeLoginAttempt(
                request,
                MockHttpServletResponse(),
                authorizationParameters(response)["state"],
            ),
        )
    }

    private fun assertLoginCookie(response: MockHttpServletResponse) {
        val cookie = MockCookie.parse(response.getHeaders(HttpHeaders.SET_COOKIE).single())
        assertEquals("__Host-test_session_login", cookie.name)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.secure)
        assertEquals("Lax", cookie.sameSite)
        assertEquals("/", cookie.path)
        assertNull(cookie.domain)
        assertEquals(420, cookie.maxAge)
    }
}
