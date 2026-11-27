package io.klibs.app.controller

import io.klibs.app.configuration.MetricsConfiguration
import io.klibs.app.service.LoginCookieService
import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.dto.CreatedSessionDTO
import io.klibs.core.user.dto.OAuthLoginCallbackDto
import io.klibs.core.user.service.HubLoginService
import io.klibs.integration.hub.enums.HubAuthenticationFailure
import io.klibs.integration.hub.exception.HubAuthenticationException
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.http.ResponseCookie
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HubAuthenticationMetricsTest {
    private val clock = MockClock()
    private val registry = SimpleMeterRegistry(SimpleConfig.DEFAULT, clock)

    @AfterEach
    fun tearDown() {
        registry.close()
    }

    @ParameterizedTest
    @ValueSource(ints = [302, 400, 504])
    fun `records callback processing time for successful and rejected attempts`(status: Int) {
        val loginCookieService = mockk<LoginCookieService>()
        every { loginCookieService.validateAndConsumeLoginAttempt(any(), any(), any()) } answers {
            clock.add(Duration.ofMillis(25))
            if (status == 400) null else OAuthLoginCallbackDto("verifier", "https://frontend.example/")
        }
        val hubLoginService = mockk<HubLoginService>()
        if (status == 504) {
            every { hubLoginService.login(any(), any()) } throws
                HubAuthenticationException(HubAuthenticationFailure.TIMEOUT)
        } else {
            every { hubLoginService.login(any(), any()) } returns
                CreatedSessionDTO("session-token", Instant.now().plusSeconds(60))
        }
        val sessionCookieService = mockk<SessionCookieService>()
        every { sessionCookieService.createSessionCookie(any(), any()) } returns
            ResponseCookie.from("session", "session-token").build()

        ApplicationContextRunner()
            .withUserConfiguration(MetricsConfiguration::class.java, ProxyConfiguration::class.java)
            .withPropertyValues("klibs.auth.hub.enabled=true")
            .withBean(SimpleMeterRegistry::class.java, { registry })
            .withBean(HubAuthenticationController::class.java, {
                HubAuthenticationController(
                    mockk(), mockk(), loginCookieService, mockk(), hubLoginService, sessionCookieService,
                )
            })
            .run { context ->
                assertNull(context.startupFailure)
                val uut = MockMvcBuilders.standaloneSetup(
                    context.getBean(HubAuthenticationController::class.java),
                ).build()

                uut.get("/auth/hub/callback") {
                    param("state", "state")
                    param("code", "code")
                }.andExpect { status { isEqualTo(status) } }

                val timer = registry.get("klibs.auth.hub.callback").timer()
                assertEquals(1, timer.count())
                assertEquals(25.0, timer.totalTime(TimeUnit.MILLISECONDS))
            }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    class ProxyConfiguration
}
