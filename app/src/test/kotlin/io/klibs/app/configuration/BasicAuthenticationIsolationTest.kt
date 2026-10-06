package io.klibs.app.configuration

import io.klibs.app.auth.SessionCookieService
import io.klibs.app.auth.TrustedOriginValidator
import io.klibs.app.configuration.properties.BasicAuthenticationProperties
import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.assertEquals

private const val SECURITY_PROBE_PATH = "/test/security/basic-principal"
private const val ACTUATOR_PROBE_PATH = "/actuator/security-test"
private const val SESSION_COOKIE_NAME = "__Host-test_session"
private const val ACTUATOR_USERNAME = "actuator-user"
private const val ACTUATOR_PASSWORD = "actuator-password"
private const val ADMIN_USERNAME = "admin-user"
private const val ADMIN_PASSWORD = "admin-password"
private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

@ActiveProfiles(profiles = ["test", "prod"], inheritProfiles = false)
@WebMvcTest(controllers = [BasicAuthenticationSecurityProbeController::class])
@ContextConfiguration(
    classes = [
        SecurityConfiguration::class,
        UserAuthenticationCorsConfiguration::class,
        UserAuthenticationSecurityConfiguration::class,
        BasicAuthenticationSecurityProbeController::class,
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
        "KLIBS_MAIN_USERNAME=admin-user",
        "KLIBS_MAIN_PASSWORD={noop}admin-password",
        "KLIBS_ELF_USERNAME=api-docs-user",
        "KLIBS_ELF_PASSWORD={noop}api-docs-password",
        "KLIBS_MED_USERNAME=actuator-user",
        "KLIBS_MED_PASSWORD={noop}actuator-password",
        "KLIBS_CAT_USERNAME=content-manager-user",
        "KLIBS_CAT_PASSWORD={noop}content-manager-password",
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=$ENCODED_HMAC_SECRET",
        "klibs.auth.trusted-frontend-origin=https://frontend.example",
        "klibs.auth.session.cookie-name=$SESSION_COOKIE_NAME",
        "klibs.auth.session.cookie-secure=true",
    ]
)
class BasicAuthenticationIsolationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sessionCookieService: SessionCookieService

    @MockitoBean
    private lateinit var trustedOriginValidator: TrustedOriginValidator

    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `session cookie does not change the Basic principal or roles`() {
        val basicOnlyResponse = mockMvc.get(SECURITY_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ACTUATOR_USERNAME, ACTUATOR_PASSWORD))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value(ACTUATOR_USERNAME) }
            jsonPath("$.authorities") { value(hasItem("ROLE_actuator")) }
        }.andReturn().response.contentAsString

        val basicWithSessionResponse = mockMvc.get(SECURITY_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ACTUATOR_USERNAME, ACTUATOR_PASSWORD))
            cookie(sessionCookie())
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value(ACTUATOR_USERNAME) }
            jsonPath("$.authorities") { value(hasItem("ROLE_actuator")) }
        }.andReturn().response.contentAsString

        assertEquals(basicOnlyResponse, basicWithSessionResponse)
        verifyUserSessionAuthenticationWasNotUsed()
    }

    @Test
    fun `session cookie alone does not authenticate a Basic protected endpoint`() {
        mockMvc.get(ACTUATOR_PROBE_PATH) {
            cookie(sessionCookie())
        }.andExpect {
            status { isUnauthorized() }
        }

        verifyUserSessionAuthenticationWasNotUsed()
    }

    @Test
    fun `session cookie does not grant a missing Basic role`() {
        mockMvc.get(ACTUATOR_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ADMIN_USERNAME, ADMIN_PASSWORD))
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.get(ACTUATOR_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ADMIN_USERNAME, ADMIN_PASSWORD))
            cookie(sessionCookie())
        }.andExpect {
            status { isForbidden() }
        }

        verifyUserSessionAuthenticationWasNotUsed()
    }

    @Test
    fun `valid Basic role works with and without a session cookie`() {
        mockMvc.get(ACTUATOR_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ACTUATOR_USERNAME, ACTUATOR_PASSWORD))
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get(ACTUATOR_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization(ACTUATOR_USERNAME, ACTUATOR_PASSWORD))
            cookie(sessionCookie())
        }.andExpect {
            status { isNoContent() }
        }

        verifyUserSessionAuthenticationWasNotUsed()
    }

    private fun verifyUserSessionAuthenticationWasNotUsed() {
        verifyNoInteractions(sessionCookieService, trustedOriginValidator, userSessionService)
    }

    private fun sessionCookie(): Cookie = Cookie(SESSION_COOKIE_NAME, "session-token")

    private fun basicAuthorization(username: String, password: String): String {
        val credentials = "$username:$password".toByteArray(StandardCharsets.UTF_8)
        return "Basic ${Base64.getEncoder().encodeToString(credentials)}"
    }
}

@RestController
internal class BasicAuthenticationSecurityProbeController {

    @GetMapping(SECURITY_PROBE_PATH)
    fun principal(authentication: Authentication): SecurityProbeResponse = SecurityProbeResponse(
        name = authentication.name,
        authorities = authentication.authorities.mapNotNull { it.authority }.sorted(),
    )

    @GetMapping(ACTUATOR_PROBE_PATH)
    fun actuatorEndpoint(): ResponseEntity<Void> = ResponseEntity.noContent().build()
}

internal data class SecurityProbeResponse(
    val name: String,
    val authorities: List<String>,
)
