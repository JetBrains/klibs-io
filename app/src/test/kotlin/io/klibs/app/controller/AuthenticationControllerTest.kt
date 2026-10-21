package io.klibs.app.controller

import BaseUnitWithDbLayerTest
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.service.UserService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestPropertySource(
    properties = [
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=$ENCODED_HMAC_SECRET",
        "klibs.auth.trusted-frontend-origin=$TRUSTED_ORIGIN",
        "klibs.auth.session.idle-ttl=30d",
        "klibs.auth.session.refresh-interval=1d",
        "klibs.auth.session.absolute-ttl=180d",
        "klibs.auth.session.cookie-name=$SESSION_COOKIE_NAME",
        "klibs.auth.session.cookie-secure=true",
    ]
)
class AuthenticationControllerTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `current-user reports an anonymous request without caching it`() {
        mockMvc.get("/auth/current-user")
            .andExpect {
                status { isOk() }
                header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
                jsonPath("$.authenticated") { value(false) }
                jsonPath("$.userId") { doesNotExist() }
            }
    }

    @Test
    fun `current-user returns the user id for a valid session`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.get("/auth/current-user") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
            header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
            jsonPath("$.authenticated") { value(true) }
            jsonPath("$.userId") { value(user.id.toString()) }
        }
    }

    @Test
    fun `current-user ignores session tokens outside cookies`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.get("/auth/current-user") {
            param(SESSION_COOKIE_NAME, session.token)
            header(HttpHeaders.AUTHORIZATION, "Bearer ${session.token}")
        }.andExpect {
            status { isOk() }
            jsonPath("$.authenticated") { value(false) }
            jsonPath("$.userId") { doesNotExist() }
        }

        assertNotNull(userSessionService.authenticateAndRefreshSessionIfAlive(session.token))
    }

    @Test
    fun `current-user reports unauthenticated for an unknown session token`() {
        mockMvc.get("/auth/current-user") {
            cookie(Cookie(SESSION_COOKIE_NAME, "unknown-session-token"))
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { exists(HttpHeaders.SET_COOKIE) }
            jsonPath("$.authenticated") { value(false) }
            jsonPath("$.userId") { doesNotExist() }
        }
    }

    @Test
    fun `sign-out deletes the session and expires its cookie`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        val response = mockMvc.post("/auth/sign-out") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
            header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
        }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { exists(HttpHeaders.SET_COOKIE) }
        }.andReturn().response

        val setCookie = response.getHeader(HttpHeaders.SET_COOKIE)
        assertNotNull(setCookie)
        assertTrue(setCookie.startsWith("$SESSION_COOKIE_NAME="))
        assertTrue(setCookie.contains("Max-Age=0"))
        assertTrue(setCookie.contains("Path=/"))
        assertTrue(setCookie.contains("Secure"))
        assertTrue(setCookie.contains("HttpOnly"))
        assertTrue(setCookie.contains("SameSite=Lax"))
        assertNull(userSessionService.authenticateAndRefreshSessionIfAlive(session.token))
    }

    @Test
    fun `sign-out cannot be triggered with a get request`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.get("/auth/sign-out") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
            header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
        }.andExpect {
            status { is4xxClientError() }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }

        assertNotNull(userSessionService.authenticateAndRefreshSessionIfAlive(session.token))
    }

    @Test
    fun `sign-out with an untrusted origin leaves the session active`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.post("/auth/sign-out") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
            header(HttpHeaders.ORIGIN, "https://attacker.example")
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }

        assertNotNull(userSessionService.authenticateAndRefreshSessionIfAlive(session.token))
    }

    @Test
    fun `sign-out with a session cookie and missing origin leaves the session active`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.post("/auth/sign-out") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
        }.andExpect {
            status { isForbidden() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }

        assertNotNull(userSessionService.authenticateAndRefreshSessionIfAlive(session.token))
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [TRUSTED_ORIGIN])
    fun `sign-out without a cookie does nothing and succeeds`(origin: String?) {
        mockMvc.post("/auth/sign-out") {
            if (origin != null) {
                header(HttpHeaders.ORIGIN, origin)
            }
        }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }
    }

    @Test
    fun `sign-out is idempotent for a stale session cookie from the trusted origin`() {
        mockMvc.post("/auth/sign-out") {
            cookie(Cookie(SESSION_COOKIE_NAME, "stale-session-token"))
            header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
        }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { exists(HttpHeaders.SET_COOKIE) }
        }
    }

    private companion object {
        val IDENTITY = ExternalUserIdentity("hub-user-42")
    }
}

private const val TRUSTED_ORIGIN = "https://frontend.example"
private const val SESSION_COOKIE_NAME = "test_session"
private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
