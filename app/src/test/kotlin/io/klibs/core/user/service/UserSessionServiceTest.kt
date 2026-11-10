package io.klibs.core.user.service

import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.entity.UserSessionEntity
import io.klibs.core.user.repository.UserSessionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserSessionServiceTest {
    private val sessionRepository = mockk<UserSessionRepository>()
    private val hashingService = AuthenticationHashingService(SECRET)
    private val clock = Clock.fixed(NOW, ZoneOffset.UTC)

    @Test
    fun `creates an opaque session with creation and expiration times`() {
        every { sessionRepository.save(any<UserSessionEntity>()) } answers { firstArg<UserSessionEntity>() }
        val user = user()
        val service = service(
            secureRandom = FixedSecureRandom(ByteArray(32) { it.toByte() }),
        )
        val createdSession = service.createSession(user)

        assertEquals("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", createdSession.token)
        assertEquals(NOW.plus(IDLE_TTL), createdSession.expiresAt)
    }

    @Test
    fun `returns an active session without refreshing it too early`() {
        val user = user()
        val session = session(user)
        every { sessionRepository.findByTokenHash(TOKEN_HASH) } returns session

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertEquals(user.id, result?.userId)
        assertNull(requireNotNull(result).refreshedCookieTtl)
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
        every { sessionRepository.findByTokenHash(TOKEN_HASH) } returns session
        every { sessionRepository.updateExpiration(session.id, NOW.plus(IDLE_TTL)) } returns 1L

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertEquals(user.id, result?.userId)
        assertEquals(IDLE_TTL, requireNotNull(result).refreshedCookieTtl)
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
        every { sessionRepository.findByTokenHash(TOKEN_HASH) } returns session
        every { sessionRepository.updateExpiration(session.id, absoluteExpiresAt) } returns 1L

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertEquals(Duration.ofDays(1), requireNotNull(result).refreshedCookieTtl)
    }

    @Test
    fun `rejects a session when its idle expiration is reached`() {
        val session = session(user(), expiresAt = NOW)
        every { sessionRepository.findByTokenHash(TOKEN_HASH) } returns session
        every { sessionRepository.deleteByTokenHash(TOKEN_HASH) } returns 1

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertNull(result)
    }

    @Test
    fun `rejects an active session when its absolute expiration is reached`() {
        val session = session(
            user = user(),
            createdAt = NOW.minus(ABSOLUTE_TTL),
            expiresAt = NOW.plus(Duration.ofDays(1)),
        )
        every { sessionRepository.findByTokenHash(TOKEN_HASH) } returns session
        every { sessionRepository.deleteByTokenHash(TOKEN_HASH) } returns 1

        val result = service().authenticateAndRefreshSessionIfAlive(TOKEN)

        assertNull(result)
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
