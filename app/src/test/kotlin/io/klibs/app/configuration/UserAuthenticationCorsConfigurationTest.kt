package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class UserAuthenticationCorsConfigurationTest {
    private val configuration = UserAuthenticationCorsConfiguration()

    @ParameterizedTest
    @CsvSource(
        "prod, https://frontend.example",
        "prod, https://frontend.example:8443",
        "local, https://frontend.example",
        "local, http://localhost:3000",
    )
    fun `configures the trusted origin for auth and user actions`(profile: String, origin: String) {
        val environment = MockEnvironment().apply {
            setActiveProfiles(profile)
        }

        val source = configuration.authenticationCorsConfigurationSource(properties(origin), environment)

        for (path in listOf(AUTHENTICATION_ENDPOINTS_PATH, USER_ACTIONS_PATH)) {
            val corsConfiguration = assertNotNull(source.corsConfigurations[path])
            assertEquals(listOf(origin), corsConfiguration.allowedOrigins)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "file://frontend.example",
            "https:/frontend.example",
            "https://user:password@frontend.example",
            "https://frontend.example/",
            "https://frontend.example?tab=security",
            "https://frontend.example#account",
            "https://frontend example",
        ]
    )
    fun `rejects invalid trusted origin configuration when creating the bean`(origin: String) {
        assertFailsWith<IllegalArgumentException> {
            configuration.authenticationCorsConfigurationSource(properties(origin), MockEnvironment())
        }
    }

    @Test
    fun `requires a secure trusted origin in production`() {
        val environment = MockEnvironment().apply {
            setActiveProfiles("prod")
        }

        assertFailsWith<IllegalArgumentException> {
            configuration.authenticationCorsConfigurationSource(properties("http://frontend.example"), environment)
        }
    }

    private fun properties(origin: String) = UserAuthenticationProperties(
        hmacSecret = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        trustedFrontendOrigin = origin,
        session = UserAuthenticationProperties.Session(
            idleTtl = Duration.ofDays(30),
            refreshInterval = Duration.ofDays(1),
            absoluteTtl = Duration.ofDays(180),
            cookieName = "__Host-test_session",
            cookieSecure = true,
        ),
    )
}
