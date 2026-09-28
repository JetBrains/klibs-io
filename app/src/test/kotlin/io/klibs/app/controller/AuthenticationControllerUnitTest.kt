package io.klibs.app.controller

import io.klibs.app.auth.AuthenticatedUserPrincipal
import io.klibs.app.auth.SessionCookieService
import io.klibs.core.user.service.UserSessionService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticationControllerUnitTest {
    private val sessionCookieService = mock<SessionCookieService>()
    private val userSessionService = mock<UserSessionService>()
    private val request = mock<HttpServletRequest>()
    private val controller = AuthenticationController(
        sessionCookieService = sessionCookieService,
        userSessionService = userSessionService,
    )

    @Test
    fun `current-user reports an anonymous principal`() {
        val response = controller.currentUser(null)

        assertFalse(response.authenticated)
        assertNull(response.userId)
        verifyNoInteractions(sessionCookieService, userSessionService)
    }

    @Test
    fun `current-user exposes the authenticated local user id`() {
        val response = controller.currentUser(AuthenticatedUserPrincipal(USER_ID))

        assertTrue(response.authenticated)
        assertEquals(USER_ID, response.userId)
        verifyNoInteractions(sessionCookieService, userSessionService)
    }

    @Test
    fun `no-cookie sign-out succeeds without origin validation or session access`() {
        whenever(sessionCookieService.getSessionToken(request)).thenReturn(null)

        val response = controller.signOut(request)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertNull(response.headers[HttpHeaders.SET_COOKIE])
        verify(sessionCookieService).getSessionToken(request)
        verifyNoMoreInteractions(sessionCookieService)
        verifyNoInteractions(userSessionService)
    }

    @Test
    fun `sign-out deletes the session and creates its deletion cookie`() {
        val deletionCookie = ResponseCookie.from("test_session", "")
            .maxAge(0)
            .build()
        whenever(sessionCookieService.getSessionToken(request)).thenReturn("session-token")
        whenever(sessionCookieService.createSessionCookieForDeletion()).thenReturn(deletionCookie)

        val response = controller.signOut(request)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertEquals(deletionCookie.toString(), response.headers.getFirst(HttpHeaders.SET_COOKIE))
        verify(sessionCookieService).getSessionToken(request)
        verify(sessionCookieService).createSessionCookieForDeletion()
        verify(userSessionService).deleteSession("session-token")
    }

    private companion object {
        val USER_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    }
}
