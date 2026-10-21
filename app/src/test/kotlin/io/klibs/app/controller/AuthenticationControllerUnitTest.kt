package io.klibs.app.controller

import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthenticationControllerUnitTest {
    private val sessionCookieService = mock<SessionCookieService>()
    private val userSessionService = mock<UserSessionService>()
    private val request = mock<HttpServletRequest>()
    private val controller = AuthenticationController(
        sessionCookieService = sessionCookieService,
        userSessionService = userSessionService,
    )

    @Test
    fun `no-cookie sign-out succeeds without origin validation or session access`() {
        whenever(sessionCookieService.getSessionToken(request)).thenReturn(null)

        val response = controller.signOut(request)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertNull(response.headers[HttpHeaders.SET_COOKIE])
        verifyNoInteractions(userSessionService)
    }
}
