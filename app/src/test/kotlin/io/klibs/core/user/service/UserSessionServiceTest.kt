package io.klibs.core.user.service

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.entity.UserSessionEntity
import io.klibs.core.user.repository.UserSessionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UserSessionServiceTest {
    private val sessionRepository = mock<UserSessionRepository>()
    private val hashingService = AuthenticationHashingService(SECRET)
    private val clock = Clock.fixed(NOW, ZoneOffset.UTC)

    @Test
    fun `creates an opaque session with creation and expiration times`() {
        val user = user()
        val service = service(
            secureRandom = FixedSecureRandom(ByteArray(32) { it.toByte() }),
        )
        whenever(
            sessionRepository.saveIfAbsent(
                any(), eq(user), any(), eq(NOW), eq(NOW.plus(IDLE_TTL))
            )
        ).thenReturn(1)

        val createdSession = service.createSession(user)

        val storedTokenHash = argumentCaptor<String>().run {
            verify(sessionRepository).saveIfAbsent(
                any(),
                eq(user),
                capture(),
                eq(NOW),
                eq(createdSession.expiresAt),
            )
            firstValue
        }
        assertEquals("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", createdSession.token)
        assertEquals(NOW.plus(IDLE_TTL), createdSession.expiresAt)
        assertEquals(hashingService.hashSessionToken(createdSession.token), storedTokenHash)
    }

    @Test
    fun `regenerates the token when its hash already exists`() {
        val user = user()
        val firstBytes = ByteArray(32)
        val secondBytes = ByteArray(32) { (it + 1).toByte() }
        whenever(
            sessionRepository.saveIfAbsent(any(), eq(user), any(), eq(NOW), eq(NOW.plus(IDLE_TTL)))
        ).thenReturn(0, 1)

        val createdSession = service(
            secureRandom = SequenceSecureRandom(firstBytes, secondBytes),
        ).createSession(user)

        val expectedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secondBytes)
        assertEquals(expectedToken, createdSession.token)
        verify(sessionRepository, times(2)).saveIfAbsent(any(), eq(user), any(), any(), any())
    }

    @Test
    fun `fails after bounded retries when the random source keeps colliding`() {
        val user = user()
        whenever(
            sessionRepository.saveIfAbsent(any(), eq(user), any(), eq(NOW), eq(NOW.plus(IDLE_TTL)))
        ).thenReturn(0)

        val exception = assertFailsWith<IllegalStateException> {
            service(secureRandom = FixedSecureRandom(ByteArray(32))).createSession(user)
        }

        assertEquals("Failed to generate a unique session token after 5 attempts", exception.message)
        verify(sessionRepository, times(5)).saveIfAbsent(any(), eq(user), any(), any(), any())
    }

    @Test
    fun `returns an active session without refreshing it too early`() {
        val user = user()
        val session = session(user)
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertSame(user, result?.user)
        assertFalse(requireNotNull(result).expirationRefreshed)
        assertEquals(IDLE_TTL, result.remainingTtl)
        verify(sessionRepository, never()).updateExpiration(any(), any(), any())
        verify(sessionRepository, never()).delete(any<UserSessionEntity>())
    }

    @Test
    fun `refreshes a session after the refresh interval`() {
        val user = user()
        val createdAt = NOW.minus(Duration.ofDays(2))
        val session = session(
            user = user,
            createdAt = createdAt,
            expiresAt = createdAt.plus(IDLE_TTL),
        )
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)
        whenever(
            sessionRepository.updateExpiration(session.id, session.expiresAt, NOW.plus(IDLE_TTL))
        ).thenReturn(1)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertSame(user, result?.user)
        assertTrue(requireNotNull(result).expirationRefreshed)
        assertEquals(IDLE_TTL, result.remainingTtl)
    }

    @Test
    fun `does not report a refresh when another request updated the session first`() {
        val createdAt = NOW.minus(Duration.ofDays(2))
        val session = session(
            user = user(),
            createdAt = createdAt,
            expiresAt = createdAt.plus(IDLE_TTL),
        )
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)
        whenever(sessionRepository.updateExpiration(any(), any(), any())).thenReturn(0)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertFalse(requireNotNull(result).expirationRefreshed)
    }

    @Test
    fun `does not refresh beyond the absolute expiration`() {
        val createdAt = NOW.minus(Duration.ofDays(179))
        val absoluteExpiresAt = NOW.plus(Duration.ofDays(1))
        val session = session(
            user = user(),
            createdAt = createdAt,
            expiresAt = NOW.plus(Duration.ofHours(12)),
        )
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)
        whenever(
            sessionRepository.updateExpiration(session.id, session.expiresAt, absoluteExpiresAt)
        ).thenReturn(1)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertTrue(requireNotNull(result).expirationRefreshed)
        assertEquals(Duration.ofDays(1), result.remainingTtl)
        verify(sessionRepository).updateExpiration(session.id, session.expiresAt, absoluteExpiresAt)
    }

    @Test
    fun `deletes a session when its idle expiration is reached`() {
        val session = session(user(), expiresAt = NOW)
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertNull(result)
        verify(sessionRepository).delete(session)
    }

    @Test
    fun `deletes an active session when its absolute expiration is reached`() {
        val session = session(
            user = user(),
            createdAt = NOW.minus(ABSOLUTE_TTL),
            expiresAt = NOW.plus(Duration.ofDays(1)),
        )
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(session)

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertNull(result)
        verify(sessionRepository).delete(session)
    }

    @Test
    fun `deleting an unknown session is idempotent`() {
        whenever(sessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(null)

        service().deleteSession(TOKEN)

        verify(sessionRepository, never()).delete(any<UserSessionEntity>())
    }

    @ParameterizedTest
    @EnumSource(InvalidSessionLifetime::class)
    fun `rejects invalid session lifetime configuration`(configuration: InvalidSessionLifetime) {
        assertFailsWith<IllegalArgumentException> {
            service(
                sessionSettings = UserSessionSettings(
                    idleTtl = configuration.idleTtl,
                    refreshInterval = configuration.refreshInterval,
                    absoluteTtl = configuration.absoluteTtl,
                )
            )
        }
    }

    private fun service(
        sessionSettings: UserSessionSettings = UserSessionSettings(
            idleTtl = IDLE_TTL,
            refreshInterval = REFRESH_INTERVAL,
            absoluteTtl = ABSOLUTE_TTL,
        ),
        secureRandom: SecureRandom = FixedSecureRandom(ByteArray(32)),
    ) = UserSessionService(
        sessionRepository = sessionRepository,
        hashingService = hashingService,
        sessionSettings = sessionSettings,
        clock = clock,
        secureRandom = secureRandom,
    )

    private fun user() = UserEntity(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        externalUserIdHash = "external-user-id-hash",
    )

    private fun session(
        user: UserEntity,
        createdAt: Instant = NOW,
        expiresAt: Instant = NOW.plus(IDLE_TTL),
    ) = UserSessionEntity(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        user = user,
        tokenHash = TOKEN_HASH,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )

    private class FixedSecureRandom(private val source: ByteArray) : SecureRandom() {
        override fun nextBytes(bytes: ByteArray) {
            require(bytes.size == source.size)
            source.copyInto(bytes)
        }
    }

    private class SequenceSecureRandom(vararg sources: ByteArray) : SecureRandom() {
        private val sources = sources.toList()
        private var index = 0

        override fun nextBytes(bytes: ByteArray) {
            val source = sources[index++]
            require(bytes.size == source.size)
            source.copyInto(bytes)
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-23T10:15:30Z")
        val IDLE_TTL: Duration = Duration.ofDays(30)
        val REFRESH_INTERVAL: Duration = Duration.ofDays(1)
        val ABSOLUTE_TTL: Duration = Duration.ofDays(180)
        val SECRET: ByteArray = "0123456789abcdef0123456789abcdef".toByteArray()
        const val TOKEN = "test-token"
        const val TOKEN_HASH = "c99f455fb84f6c109473b11de7df05f2053d9a09dcafd6b38ee45e50d60e0be6"
    }
}

enum class InvalidSessionLifetime(
    val idleTtl: Duration = Duration.ofDays(30),
    val refreshInterval: Duration = Duration.ofDays(1),
    val absoluteTtl: Duration = Duration.ofDays(180),
) {
    ZERO_IDLE_TTL(idleTtl = Duration.ZERO),
    ZERO_REFRESH_INTERVAL(refreshInterval = Duration.ZERO),
    REFRESH_INTERVAL_EQUAL_TO_IDLE(refreshInterval = Duration.ofDays(30)),
    ABSOLUTE_TTL_SHORTER_THAN_IDLE(absoluteTtl = Duration.ofDays(30).minusSeconds(1)),
}
