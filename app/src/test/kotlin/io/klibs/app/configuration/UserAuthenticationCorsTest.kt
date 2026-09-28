package io.klibs.app.configuration

import BaseUnitWithDbLayerTest
import io.klibs.core.user.model.AuthenticationProvider
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.service.UserService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertNotNull

@TestPropertySource(
    properties = [
        "klibs.auth.enabled=true",
        "klibs.auth.hmac-secret=$ENCODED_HMAC_SECRET",
        "klibs.auth.trusted-frontend-origin=$TRUSTED_ORIGIN",
        "klibs.auth.session.idle-ttl=30d",
        "klibs.auth.session.refresh-interval=1d",
        "klibs.auth.session.absolute-ttl=180d",
        "klibs.auth.session.cookie-name=$SESSION_COOKIE_NAME",
        "klibs.auth.session.cookie-secure=true",
    ]
)
class UserAuthenticationCorsTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `current-user is not exposed to an untrusted origin`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)

        mockMvc.get("/auth/current-user") {
            cookie(Cookie(SESSION_COOKIE_NAME, session.token))
            header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) }
        }

        assertNotNull(userSessionService.authenticateAndRefreshSession(session.token))
    }

    @Test
    fun `untrusted origin is rejected before no-cookie sign-out`() {
        mockMvc.post("/auth/sign-out") {
            header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }
    }

    @Test
    fun `preflight allows the trusted frontend to send credentials`() {
        mockMvc.perform(
            options("/auth/sign-out")
                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        )
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
    }

    @Test
    fun `preflight rejects an untrusted frontend`() {
        mockMvc.perform(
            options("/auth/sign-out")
                .header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
        )
            .andExpect(status().isForbidden)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }

    @Test
    fun `authentication CORS does not expose other protected endpoints`() {
        mockMvc.perform(
            options("/")
                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        )
            .andExpect(status().isUnauthorized)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }

    private companion object {
        val IDENTITY = ExternalUserIdentity(AuthenticationProvider.JETBRAINS_HUB, "hub-user-42")
    }
}

private const val TRUSTED_ORIGIN = "https://frontend.example"
private const val UNTRUSTED_ORIGIN = "https://attacker.example"
private const val SESSION_COOKIE_NAME = "test_session"
private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
