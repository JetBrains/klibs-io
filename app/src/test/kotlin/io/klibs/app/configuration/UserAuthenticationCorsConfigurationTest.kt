package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserAuthenticationCorsConfigurationTest {
    private val configuration = UserAuthenticationCorsConfiguration()

    @ParameterizedTest
    @ValueSource(strings = ["https://frontend.example", "https://frontend.example:8443", "http://localhost:3000"])
    fun `configures the trusted origin for auth and user actions`(origin: String) {
        val source = configuration.authenticationCorsConfigurationSource(properties(origin))

        for (path in listOf(AUTHENTICATION_ENDPOINTS_PATH, USER_ACTIONS_PATH)) {
            val corsConfiguration = assertNotNull(source.corsConfigurations[path])
            assertEquals(listOf(origin), corsConfiguration.allowedOrigins)
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
