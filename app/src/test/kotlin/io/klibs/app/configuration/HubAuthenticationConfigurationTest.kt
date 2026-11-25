package io.klibs.app.configuration

import io.klibs.app.configuration.properties.HubAuthenticationProperties
import io.klibs.app.service.LoginCookieService
import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.service.FrontendRedirectService
import io.klibs.core.user.service.OAuthLoginAttemptService
import io.klibs.core.user.service.OAuthLoginSettings
import io.klibs.core.user.service.PkceService
import io.klibs.integration.hub.configuration.HubClientSettings
import jakarta.servlet.http.Cookie
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HubAuthenticationConfigurationTest {
    private val uut = ApplicationContextRunner()
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withUserConfiguration(
            UserAuthenticationConfiguration::class.java,
            PkceService::class.java,
            FrontendRedirectService::class.java,
            OAuthLoginAttemptService::class.java,
            SessionCookieService::class.java,
            LoginCookieService::class.java,
        )
        .withPropertyValues(
            "klibs.auth.hub.enabled=true",
            "klibs.auth.hmac-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            "klibs.auth.trusted-frontend-origin=https://frontend.example",
            "klibs.auth.session.cookie-name=__Host-test_session",
            "klibs.auth.session.cookie-secure=true",
            "klibs.auth.hub.base-url=https://hub.example/hub",
            "klibs.auth.hub.client-id=test-klibs-client",
            "klibs.auth.hub.scope=openid",
            "klibs.auth.hub.callback-url=https://api.example/auth/hub/callback",
        )

    @Test
    fun `Spring wires the login protection services using configured settings`() {
        uut.run { context ->
            assertNull(context.startupFailure)
            val attemptService = context.getBean(OAuthLoginAttemptService::class.java)
            val cookieService = context.getBean(LoginCookieService::class.java)
            val attempt = attemptService.createLoginAttempt("/project/JetBrains/kotlin")
            val cookie = cookieService.createLoginCookie(attempt)
            val request = MockHttpServletRequest().apply { setCookies(Cookie(cookie.name, cookie.value)) }
            val response = MockHttpServletResponse()

            val validated = assertNotNull(
                cookieService.validateAndConsumeLoginAttempt(request, response, attempt.state),
            )

            assertEquals("https://frontend.example/project/JetBrains/kotlin", validated.redirectUrl)
            assertEquals(attempt.codeVerifier, validated.codeVerifier)
            assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE))
        }
    }

    @Test
    fun `binds Hub settings and custom timeouts`() {
        uut.withPropertyValues(
            "klibs.auth.hub.scope=openid profile",
            "klibs.auth.hub.connect-timeout=1700ms",
            "klibs.auth.hub.request-timeout=4s",
            "klibs.auth.hub.callback-timeout=9s",
            "klibs.auth.hub.login-attempt-ttl=7m",
        ).run { context ->
            assertNull(context.startupFailure)
            val properties = context.getBean(HubAuthenticationProperties::class.java)
            assertEquals("https://hub.example/hub", properties.baseUrl)
            assertEquals("test-klibs-client", properties.clientId)
            assertEquals("openid profile", properties.scope)
            assertEquals("https://api.example/auth/hub/callback", properties.callbackUrl)
            assertEquals(Duration.ofMillis(1700), properties.connectTimeout)
            assertEquals(Duration.ofSeconds(4), properties.requestTimeout)
            assertEquals(Duration.ofSeconds(9), properties.callbackTimeout)
            assertEquals(Duration.ofMinutes(7), properties.loginAttemptTtl)
            val settings = context.getBean(OAuthLoginSettings::class.java)
            assertEquals(Duration.ofMinutes(7), settings.attemptTtl)
            assertEquals("https://frontend.example", settings.trustedFrontendOrigin)
            val hubSettings = context.getBean(HubClientSettings::class.java)
            assertEquals("openid profile", hubSettings.scope, "scope must reach the Hub client settings")
            assertEquals(Duration.ofSeconds(4), hubSettings.requestTimeout, "Hub request-timeout=4s")
            assertEquals(Duration.ofSeconds(9), hubSettings.callbackTimeout, "Hub callback-timeout=9s")
            val client = context.getBean(OkHttpClient::class.java)
            assertEquals(1700, client.connectTimeoutMillis, "connect-timeout=1700ms must reach the HTTP client")
            assertEquals(4000, client.readTimeoutMillis, "request-timeout=4s must reach the HTTP read timeout")
            assertEquals(4000, client.callTimeoutMillis, "request-timeout=4s must reach the HTTP call timeout")
        }
    }

    @Test
    fun `configured Hub client disables redirects and retries of single-use authorization codes`() {
        uut.run { context ->
            assertNull(context.startupFailure)
            val client = context.getBean(OkHttpClient::class.java)
            assertFalse(client.followRedirects, "Hub credentials must not follow HTTP redirects")
            assertFalse(client.followSslRedirects, "Hub credentials must not follow redirects between HTTP and HTTPS")
            assertFalse(client.retryOnConnectionFailure, "A single-use authorization code must not be retried")
        }
    }

    @Test
    fun `loads default Hub timeouts from application configuration`() {
        uut.run { context ->
            assertNull(context.startupFailure)
            val properties = context.getBean(HubAuthenticationProperties::class.java)
            assertEquals(Duration.ofSeconds(2), properties.connectTimeout)
            assertEquals(Duration.ofSeconds(5), properties.requestTimeout)
            assertEquals(Duration.ofSeconds(10), properties.callbackTimeout)
            assertEquals(Duration.ofMinutes(10), properties.loginAttemptTtl)
        }
    }

    @Test
    fun `disabled authentication starts without Hub credentials`() {
        uut.withPropertyValues(
            "klibs.auth.hub.enabled=false",
            "klibs.auth.hub.base-url=",
            "klibs.auth.hub.client-id=",
            "klibs.auth.hub.callback-url=",
        ).run { context ->
            assertNull(context.startupFailure)
            assertEquals(0, context.getBeansOfType(HubAuthenticationProperties::class.java).size)
            assertEquals(0, context.getBeansOfType(OAuthLoginAttemptService::class.java).size)
            assertEquals(0, context.getBeansOfType(LoginCookieService::class.java).size)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["base-url", "client-id", "scope", "callback-url"])
    fun `enabled authentication rejects a blank required Hub property`(property: String) {
        uut.withPropertyValues("klibs.auth.hub.$property= ").run { context ->
            assertNotNull(context.startupFailure, "Expected startup to reject a blank $property")
        }
    }

    @Test
    fun `local profile supplies localhost Hub and callback defaults`() {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(UserAuthenticationConfiguration::class.java)
            .withPropertyValues(
                "spring.profiles.active=local",
                "klibs.auth.hub.enabled=true",
                "klibs.auth.hmac-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "klibs.auth.trusted-frontend-origin=http://localhost:3000",
            ).run { context ->
                assertNull(context.startupFailure)
                val properties = context.getBean(HubAuthenticationProperties::class.java)
                assertEquals("http://localhost:7777/hub", properties.baseUrl)
                assertEquals("klibs-io-local", properties.clientId)
                assertEquals("http://localhost:8080/auth/hub/callback", properties.callbackUrl)
                assertEquals("openid profile email", properties.scope)
            }
    }

    @Test
    fun `accepts HTTPS Hub settings in production`() {
        uut.withPropertyValues("spring.profiles.active=prod").run { context ->
            assertNull(context.startupFailure)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "base-url, http://hub.example/hub",
        "callback-url, http://api.example/auth/hub/callback",
    )
    fun `production rejects an HTTP Hub URL`(property: String, value: String) {
        uut.withPropertyValues(
            "spring.profiles.active=prod",
            "klibs.auth.hub.$property=$value",
        ).run { context ->
            assertNotNull(context.startupFailure, "Expected production to reject $property=$value")
        }
    }

    @Test
    fun `rejects a zero Hub request timeout because it disables the limit`() {
        uut.withPropertyValues("klibs.auth.hub.request-timeout=0s").run { context ->
            assertNotNull(context.startupFailure, "Expected startup to reject request-timeout=0s")
        }
    }
}
