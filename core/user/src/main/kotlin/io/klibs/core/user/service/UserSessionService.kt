package io.klibs.core.user.service

import io.klibs.core.user.dto.AuthenticatedSession
import io.klibs.core.user.dto.CreatedSession
import io.klibs.core.user.entity.UserEntity
import io.klibs.core.user.repository.UserSessionRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.UUID

@Service
@ConditionalOnProperty("klibs.auth.hub.enabled", havingValue = "true")
@Transactional
class UserSessionService(
    private val sessionRepository: UserSessionRepository,
    private val hashingService: AuthenticationHashingService,
    private val sessionSettings: UserSessionSettings,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun createSession(user: UserEntity): CreatedSession {
        val createdAt = clock.instant()
        val expiresAt = createdAt.plus(sessionSettings.idleTtl)

        repeat(MAX_TOKEN_GENERATION_ATTEMPTS) {
            val token = generateToken()
            val insertedRows = sessionRepository.saveIfAbsent(
                id = UUID.randomUUID(),
                user = user,
                tokenHash = hashingService.hashSessionToken(token),
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
            if (insertedRows > 0) {
                return CreatedSession(token = token, expiresAt = expiresAt)
            }
        }

        throw IllegalStateException(
            "Failed to generate a unique session token after $MAX_TOKEN_GENERATION_ATTEMPTS attempts"
        )
    }

    fun authenticateAndRefreshSessionIfAlive(token: String): AuthenticatedSession? {
        val tokenHash = hashingService.hashSessionToken(token)
        val session = sessionRepository.findByTokenHash(tokenHash) ?: return null
        val now = clock.instant()
        val absoluteExpiresAt = session.createdAt.plus(sessionSettings.absoluteTtl)

        // Reject sessions that exceeded either the idle or absolute lifetime.
        if (!session.expiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now)) {
            sessionRepository.delete(session)
            return null
        }

        // Refresh at most once per interval instead of writing on every request.
        val refreshTime = session.expiresAt
            .minus(sessionSettings.idleTtl)
            .plus(sessionSettings.refreshInterval)
        if (now.isBefore(refreshTime)) {
            return AuthenticatedSession(
                user = session.user,
                expirationRefreshed = false,
                remainingTtl = Duration.between(now, session.expiresAt),
            )
        }

        // Never extend a session beyond its absolute lifetime.
        val refreshedExpiresAt = minOf(now.plus(sessionSettings.idleTtl), absoluteExpiresAt)
        if (!refreshedExpiresAt.isAfter(session.expiresAt)) {
            return AuthenticatedSession(
                user = session.user,
                expirationRefreshed = false,
                remainingTtl = Duration.between(now, session.expiresAt),
            )
        }

        val expirationRefreshed = sessionRepository.updateExpiration(
            id = session.id,
            currentExpiresAt = session.expiresAt,
            newExpiresAt = refreshedExpiresAt,
        ) > 0

        val effectiveExpiresAt = if (expirationRefreshed) refreshedExpiresAt else session.expiresAt
        return AuthenticatedSession(
            user = session.user,
            expirationRefreshed = expirationRefreshed,
            remainingTtl = Duration.between(now, effectiveExpiresAt),
        )
    }

    fun deleteSession(token: String) {
        val tokenHash = hashingService.hashSessionToken(token)
        val session = sessionRepository.findByTokenHash(tokenHash) ?: return
        sessionRepository.delete(session)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_SIZE_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val TOKEN_SIZE_BYTES = 32
        const val MAX_TOKEN_GENERATION_ATTEMPTS = 5
    }
}
