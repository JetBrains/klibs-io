package io.klibs.core.user.service

import BaseUnitWithDbLayerTest
import io.klibs.core.user.entity.UserSessionEntity
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.core.user.repository.UserRepository
import io.klibs.core.user.repository.UserSessionRepository
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
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

        assertNotEquals(IDENTITY.externalUserId, storedUser.externalUserIdHash)
        assertEquals(64, storedUser.externalUserIdHash.length)
        assertNotNull(storedSession)
        assertNotEquals(session.token, storedSession.tokenHash)
        assertNull(sessionRepository.findByTokenHash(session.token))
    }

    @Test
    fun `expired database session does not authenticate and is deleted`() {
        val user = userService.findOrCreate(IDENTITY)
        val rawToken = "expired-token"
        val tokenHash = hashingService.hashSessionToken(rawToken)
        sessionRepository.save(
            UserSessionEntity(
                user = user,
                tokenHash = tokenHash,
                createdAt = Instant.now().minusSeconds(60),
                expiresAt = Instant.now().minusSeconds(1),
            )
        )

        assertNull(userSessionService.authenticateAndRefreshSessionIfAlive(rawToken))
        assertNull(sessionRepository.findByTokenHash(tokenHash))
    }

    @Test
    fun `refreshing a session persists its extended expiration`() {
        val user = userService.findOrCreate(IDENTITY)
        val rawToken = "renewable-token"
        val tokenHash = hashingService.hashSessionToken(rawToken)
        val createdAt = NOW.minus(Duration.ofDays(2))
        val expiresAt = createdAt.plus(SESSION_IDLE_TTL)
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

        assertEquals(SESSION_IDLE_TTL, result.refreshedCookieTtl)
        assertEquals(
            NOW.plus(SESSION_IDLE_TTL),
            sessionRepository.findByTokenHash(tokenHash)?.expiresAt,
        )
    }

    @Test
    fun `deleting a user cascades to all of their sessions`() {
        val user = userService.findOrCreate(IDENTITY)
        userSessionService.createSession(user)
        userSessionService.createSession(user)
        assertEquals(2, sessionRepository.count())

        userRepository.deleteById(user.id)

        assertEquals(0, sessionRepository.count())
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

    private fun sessionService() = UserSessionService(
        sessionRepository = sessionRepository,
        hashingService = hashingService,
        sessionSettings = UserSessionSettings(
            idleTtl = SESSION_IDLE_TTL,
            refreshInterval = Duration.ofDays(1),
            absoluteTtl = Duration.ofDays(180),
        ),
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    private fun <T : Any> inTransaction(block: () -> T): T =
        requireNotNull(TransactionTemplate(transactionManager).execute { block() })

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-23T10:15:30Z")
        val SESSION_IDLE_TTL: Duration = Duration.ofDays(30)
        val IDENTITY = ExternalUserIdentity("concurrent-hub-user")
    }
}

private const val ENCODED_HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
