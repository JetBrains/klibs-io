package io.klibs.core.user.service

import BaseUnitWithDbLayerTest
import io.klibs.core.user.entity.UserSessionEntity
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.UserRepository
import io.klibs.core.user.repository.UserSessionRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestPropertySource(
    properties = [
        "klibs.auth.hub.enabled=true",
        "klibs.auth.hmac-secret=$ENCODED_HMAC_SECRET",
        "klibs.auth.trusted-frontend-origin=https://frontend.example",
        "klibs.auth.session.idle-ttl=30d",
        "klibs.auth.session.refresh-interval=1d",
        "klibs.auth.session.absolute-ttl=180d",
        "klibs.auth.session.cookie-name=test_session",
        "klibs.auth.session.cookie-secure=true",
    ]
)
class UserPersistenceIntegrationTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var sessionSettings: UserSessionSettings

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userSessionService: UserSessionService

    @Autowired
    private lateinit var hashingService: AuthenticationHashingService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var sessionRepository: UserSessionRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `concurrent logins for the same identity create one local user`() {
        val workerCount = 12
        val startBarrier = CyclicBarrier(workerCount)
        val executor = Executors.newFixedThreadPool(workerCount)

        try {
            val futures = List(workerCount) {
                executor.submit<UUID> {
                    startBarrier.await(10, TimeUnit.SECONDS)
                    userService.findOrCreate(IDENTITY).id
                }
            }

            val userIds = futures.map { it.get(20, TimeUnit.SECONDS) }

            assertEquals(1, userIds.toSet().size)
            assertEquals(1, userRepository.count())
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `different external identities create different local users`() {
        val firstUser = userService.findOrCreate(IDENTITY)
        val secondUser = userService.findOrCreate(
            IDENTITY.copy(externalUserId = "another-hub-user")
        )

        assertNotEquals(firstUser.id, secondUser.id)
        assertEquals(2, userRepository.count())
    }

    @Test
    fun `session token hash uniqueness is enforced without replacing the existing session`() {
        val user = userService.findOrCreate(IDENTITY)
        val tokenHash = hashingService.hashSessionToken("colliding-token")
        val createdAt = Instant.now()
        val expiresAt = Instant.now().plusSeconds(60)

        val firstSession = saveSession(user.id, tokenHash, createdAt, expiresAt)

        assertFailsWith<DataIntegrityViolationException> {
            saveSession(user.id, tokenHash, createdAt, expiresAt)
        }
        assertEquals(firstSession.id, sessionRepository.findByTokenHash(tokenHash)?.id)
        assertEquals(1, sessionRepository.count())
    }

    @Test
    fun `raw external id and raw session token are not stored`() {
        val user = userService.findOrCreate(IDENTITY)
        val session = userSessionService.createSession(user)
        val tokenHash = hashingService.hashSessionToken(session.token)

        val storedUser = userRepository.findById(user.id).orElseThrow()
        val storedSession = sessionRepository.findByTokenHash(tokenHash)

        assertEquals(hashingService.hashExternalIdentity(IDENTITY), storedUser.externalUserIdHash)
        assertNotNull(storedSession)
        assertNotEquals(session.token, storedSession.tokenHash)
        assertNull(sessionRepository.findByTokenHash(session.token))
    }

    @Test
    fun `expired database session does not authenticate and is deleted`() {
        val user = userService.findOrCreate(IDENTITY)
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val controlSessions = saveControlSessions(user.id, now)
        val rawToken = "expired-token"
        val tokenHash = hashingService.hashSessionToken(rawToken)
        sessionRepository.save(
            UserSessionEntity(
                user = user,
                tokenHash = tokenHash,
                createdAt = now.minusSeconds(60),
                expiresAt = now.minusSeconds(1),
            )
        )

        assertNull(userSessionService.authenticateAndRefreshSessionIfAlive(rawToken))
        assertNull(sessionRepository.findByTokenHash(tokenHash))
        assertSessionsUnchanged(controlSessions)
    }

    @Test
    fun `session past absolute lifetime does not authenticate and is deleted`() {
        val user = userService.findOrCreate(IDENTITY)
        val rawToken = "absolute-ttl-expired-token"
        val tokenHash = hashingService.hashSessionToken(rawToken)
        val now = Instant.now()

        sessionRepository.save(
            UserSessionEntity(
                user = user,
                tokenHash = tokenHash,
                createdAt = now
                    .minus(sessionSettings.absoluteTtl)
                    .minusSeconds(1),
                expiresAt = now.plus(Duration.ofDays(1)),
            )
        )

        assertNull(userSessionService.authenticateAndRefreshSessionIfAlive(rawToken))
        assertNull(sessionRepository.findByTokenHash(tokenHash))
    }

    @Test
    fun `refreshing a session persists its extended expiration`() {
        val user = userService.findOrCreate(IDENTITY)
        val controlSessions = saveControlSessions(user.id, NOW)
        val rawToken = "renewable-token"
        val tokenHash = hashingService.hashSessionToken(rawToken)
        val createdAt = NOW.minus(Duration.ofDays(2))
        val expiresAt = createdAt.plus(sessionSettings.idleTtl)
        sessionRepository.save(
            UserSessionEntity(
                user = user,
                tokenHash = tokenHash,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        )

        val result = inTransaction {
            requireNotNull(
                sessionService()
                    .authenticateAndRefreshSessionIfAlive(rawToken)
            )
        }

        assertEquals(sessionSettings.idleTtl, result.refreshedCookieTtl)
        assertEquals(
            NOW.plus(sessionSettings.idleTtl),
            sessionRepository.findByTokenHash(tokenHash)?.expiresAt,
        )
        assertSessionsUnchanged(controlSessions)
    }

    @Test
    fun `deleting a user cascades only to their sessions`() {
        val user = userService.findOrCreate(IDENTITY)
        val otherUser = userService.findOrCreate(OTHER_IDENTITY)
        userSessionService.createSession(user)
        userSessionService.createSession(user)
        val otherUserSession = userSessionService.createSession(otherUser)
        assertEquals(3, sessionRepository.count())

        userRepository.deleteById(user.id)

        assertEquals(1, sessionRepository.count())
        assertNotNull(userSessionService.authenticateAndRefreshSessionIfAlive(otherUserSession.token))
    }

    private fun saveSession(
        userId: UUID,
        tokenHash: String,
        createdAt: Instant,
        expiresAt: Instant,
    ): UserSessionEntity = inTransaction {
        val user = userRepository.findById(userId).orElseThrow()
        sessionRepository.save(
            UserSessionEntity(
                user = user,
                tokenHash = tokenHash,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
        )
    }

    private fun saveControlSessions(
        userId: UUID,
        createdAt: Instant,
    ): List<UserSessionEntity> {
        val otherUser = userService.findOrCreate(OTHER_IDENTITY)
        return listOf(
            saveSession(
                userId = userId,
                tokenHash = hashingService.hashSessionToken("same-user-control-token"),
                createdAt = createdAt,
                expiresAt = createdAt.plus(Duration.ofDays(10)),
            ),
            saveSession(
                userId = otherUser.id,
                tokenHash = hashingService.hashSessionToken("other-user-control-token"),
                createdAt = createdAt,
                expiresAt = createdAt.plus(Duration.ofDays(20)),
            ),
        )
    }

    private fun assertSessionsUnchanged(sessions: List<UserSessionEntity>) {
        sessions.forEach { expected ->
            val actual = assertNotNull(sessionRepository.findByTokenHash(expected.tokenHash))
            assertEquals(expected.id, actual.id)
            assertEquals(expected.expiresAt, actual.expiresAt)
        }
    }

    private fun sessionService() = UserSessionService(
        sessionRepository = sessionRepository,
        hashingService = hashingService,
        sessionSettings = sessionSettings,
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    private fun <T : Any> inTransaction(block: () -> T): T =
        requireNotNull(TransactionTemplate(transactionManager).execute { block() })

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-23T10:15:30Z")
        val IDENTITY = ExternalUserIdentity("concurrent-hub-user")
        val OTHER_IDENTITY = ExternalUserIdentity("other-hub-user")
    }
}

private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
