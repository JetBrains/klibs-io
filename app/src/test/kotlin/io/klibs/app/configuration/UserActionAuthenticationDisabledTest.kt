package io.klibs.app.configuration

import io.klibs.app.auth.RequiresAuthenticatedUser
import io.klibs.app.configuration.properties.AuthProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.Base64

private const val DISABLED_USER_ACTION_PATH = "/user/disabled-security-test"
private const val METHOD_SECURITY_PROBE_PATH = "/test/security/requires-klibs-user"
private const val SESSION_COOKIE_NAME = "klibs_session"
private const val BASIC_USERNAME = "basic-user"
private const val BASIC_PASSWORD = "basic-password"

@ActiveProfiles("test")
@WebMvcTest(controllers = [UserActionAuthenticationDisabledProbeController::class])
@ContextConfiguration(
    classes = [
        SecurityConfiguration::class,
        UserAuthenticationCorsConfiguration::class,
        UserAuthenticationSecurityConfiguration::class,
        UserActionAuthenticationDisabledProbeController::class,
    ]
)
@ImportAutoConfiguration(
    SecurityAutoConfiguration::class,
    SecurityFilterAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
)
@EnableConfigurationProperties(AuthProperties::class)
@TestPropertySource(
    properties = [
        "klibs.auth.enabled=false",
        "klibs.auth.users[0].username=$BASIC_USERNAME",
        "klibs.auth.users[0].password={noop}$BASIC_PASSWORD",
        "klibs.auth.users[0].roles[0]=ADMIN",
    ]
)
class UserActionAuthenticationDisabledTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `user actions reject anonymous requests when authentication is disabled`() {
        mockMvc.get(DISABLED_USER_ACTION_PATH)
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `user actions ignore session cookies when authentication is disabled`() {
        mockMvc.get(DISABLED_USER_ACTION_PATH) {
            cookie(Cookie(SESSION_COOKIE_NAME, "session-token"))
        }.andExpect {
            status { isUnauthorized() }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }
    }

    @Test
    fun `user actions reject Basic credentials when authentication is disabled`() {
        mockMvc.get(DISABLED_USER_ACTION_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `authenticated-user annotation remains active when authentication is disabled`() {
        mockMvc.get(METHOD_SECURITY_PROBE_PATH) {
            header(HttpHeaders.AUTHORIZATION, basicAuthorization())
        }.andExpect {
            status { isForbidden() }
        }
    }

    private fun basicAuthorization(): String {
        val credentials = "$BASIC_USERNAME:$BASIC_PASSWORD"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encodedCredentials"
    }
}

@RestController
internal class UserActionAuthenticationDisabledProbeController {
    @GetMapping(DISABLED_USER_ACTION_PATH)
    fun userAction(): ResponseEntity<Void> = ResponseEntity.noContent().build()

    @GetMapping(METHOD_SECURITY_PROBE_PATH)
    @RequiresAuthenticatedUser
    fun methodSecurityProbe(): ResponseEntity<Void> = ResponseEntity.noContent().build()
}
