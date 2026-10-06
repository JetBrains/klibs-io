package io.klibs.app.configuration

import io.klibs.app.auth.AuthenticatedUserPrincipal
import io.klibs.app.auth.RequiresAuthenticatedUser
import io.klibs.app.auth.SessionCookieService
import io.klibs.app.configuration.properties.BasicAuthenticationProperties
import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.dto.AuthenticatedSession
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID

private const val USER_ACTION_PATH = "/user/security-test"
private const val SESSION_COOKIE_NAME = "test_session"
private const val SESSION_TOKEN = "session-token"
private const val TRUSTED_ORIGIN = "https://frontend.example"
private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

@ActiveProfiles("test")
@WebMvcTest(controllers = [UserActionSecurityProbeController::class])
@ContextConfiguration(
    classes = [
        SecurityConfiguration::class,
        UserAuthenticationCorsConfiguration::class,
        UserAuthenticationSecurityConfiguration::class,
        UserAuthenticationConfiguration::class,
        UserActionSecurityProbeController::class,
        SessionCookieService::class,
    ]
)
@ImportAutoConfiguration(
    SecurityAutoConfiguration::class,
    SecurityFilterAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
)
@EnableConfigurationProperties(
    value = [
        BasicAuthenticationProperties::class,
        UserAuthenticationProperties::class,
    ]
)
@TestPropertySource(
    properties = [
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=$ENCODED_HMAC_SECRET",
        "klibs.auth.trusted-frontend-origin=$TRUSTED_ORIGIN",
        "klibs.auth.session.cookie-name=$SESSION_COOKIE_NAME",
        "klibs.auth.session.cookie-secure=true",
    ]
)
class UserActionSecurityTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `protected action rejects a request without a session cookie`() {
        mockMvc.get(USER_ACTION_PATH)
            .andExpect {
                status { isUnauthorized() }
                header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            }

        verifyNoInteractions(userSessionService)
    }

    @Test
    fun `protected action receives the authenticated local user`() {
        whenever(userSessionService.authenticateAndRefreshSessionIfAlive(SESSION_TOKEN))
            .thenReturn(authenticatedSession())

        mockMvc.get(USER_ACTION_PATH) {
            cookie(sessionCookie())
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            jsonPath("$.userId") { value(USER_ID.toString()) }
        }
    }

    @Test
    fun `unsafe action rejects a missing origin before session lookup`() {
        mockMvc.post(USER_ACTION_PATH) {
            cookie(sessionCookie())
        }.andExpect {
            status { isForbidden() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
        }

        verifyNoInteractions(userSessionService)
    }

    @Test
    fun `unsafe action rejects an untrusted origin before session lookup`() {
        mockMvc.post(USER_ACTION_PATH) {
            cookie(sessionCookie())
            header(HttpHeaders.ORIGIN, "https://attacker.example")
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) }
        }

        verifyNoInteractions(userSessionService)
    }

    @Test
    fun `unsafe action accepts a valid session from the trusted origin`() {
        whenever(userSessionService.authenticateAndRefreshSessionIfAlive(SESSION_TOKEN))
            .thenReturn(authenticatedSession())

        mockMvc.post(USER_ACTION_PATH) {
            cookie(sessionCookie())
            header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
        }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
        }
    }

    @Test
    fun `unknown session is rejected and its cookie is expired`() {
        whenever(userSessionService.authenticateAndRefreshSessionIfAlive(SESSION_TOKEN)).thenReturn(null)

        mockMvc.get(USER_ACTION_PATH) {
            cookie(sessionCookie())
        }.andExpect {
            status { isUnauthorized() }
            header { string(HttpHeaders.CACHE_CONTROL, containsString("no-store")) }
            header { string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")) }
        }
    }

    @Test
    fun `Basic credentials cannot replace a user session`() {
        mockMvc.get(USER_ACTION_PATH) {
            header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==")
        }.andExpect {
            status { isUnauthorized() }
        }

        verifyNoInteractions(userSessionService)
    }

    private fun sessionCookie(): Cookie = Cookie(SESSION_COOKIE_NAME, SESSION_TOKEN)

    private fun authenticatedSession() = AuthenticatedSession(
        user = UserEntity(
            id = USER_ID,
            externalUserIdHash = "external-user-id-hash",
        ),
        expirationRefreshed = false,
        remainingTtl = Duration.ofDays(1),
    )

    private companion object {
        val USER_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    }
}

@RestController
internal class UserActionSecurityProbeController {
    @GetMapping(USER_ACTION_PATH)
    @RequiresAuthenticatedUser
    fun read(
        @AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
    ): UserActionSecurityProbeResponse = UserActionSecurityProbeResponse(principal.userId)

    @PostMapping(USER_ACTION_PATH)
    @RequiresAuthenticatedUser
    fun write(): ResponseEntity<Void> = ResponseEntity.noContent().build()
}

internal class UserActionSecurityProbeResponse(
    val userId: UUID,
)
