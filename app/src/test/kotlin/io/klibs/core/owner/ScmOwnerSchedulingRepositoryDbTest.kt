package io.klibs.core.owner

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FAILURE_REASON = "GitHubApiException: GitHub returned 503"
private const val DELETED_REASON = "ScmOwnerDeletedException: login=voize-gmbh nativeId=62517686"

private const val SEED = "classpath:sql/ScmOwnerSchedulingRepositoryDbTest/insert-owners-for-update.sql"

private const val VOIZE_ID = 372
private const val XING_ID = 373

class ScmOwnerSchedulingRepositoryDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: ScmOwnerSchedulingRepository

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Test
    @Sql(SEED)
    fun `an owner with no scheduling row has no backoff state`() {
        assertNull(uut.find(VOIZE_ID))
    }

    @Test
    @Sql(SEED)
    fun `deferring an owner stores its attempts, reason and future retry time`() {
        val before = Instant.now()

        uut.scheduleNextRetry(VOIZE_ID, attempts = 1, backoffDelaySeconds = 60, FAILURE_REASON)

        val deferred = assertNotNull(uut.find(VOIZE_ID))
        assertEquals(VOIZE_ID, deferred.scmOwnerId)
        assertEquals(1, deferred.retryAttempts)
        assertEquals(FAILURE_REASON, deferred.reason)
        val nextRetryAt = assertNotNull(deferred.nextRetryAt)
        assertTrue(
            nextRetryAt.isAfter(before.plusSeconds(30)),
            "next_retry_at=$nextRetryAt should be roughly 60s past $before"
        )
    }

    @Test
    @Sql(SEED)
    fun `deferring an already deferred owner replaces its state instead of failing`() {
        uut.scheduleNextRetry(VOIZE_ID, attempts = 1, backoffDelaySeconds = 60, FAILURE_REASON)
        uut.scheduleNextRetry(VOIZE_ID, attempts = 2, backoffDelaySeconds = 120, DELETED_REASON)

        val deferred = assertNotNull(uut.find(VOIZE_ID))
        assertEquals(2, deferred.retryAttempts)
        assertEquals(DELETED_REASON, deferred.reason)
    }

    @Test
    @Sql(SEED)
    fun `clearing the schedule removes the backoff state`() {
        uut.scheduleNextRetry(VOIZE_ID, attempts = 3, backoffDelaySeconds = 3600, FAILURE_REASON)

        uut.clearSchedule(VOIZE_ID)

        assertNull(uut.find(VOIZE_ID))
    }

    @Test
    @Sql(SEED)
    fun `a deferred owner is not selected for update`() {
        uut.scheduleNextRetry(VOIZE_ID, attempts = 1, backoffDelaySeconds = 3600, FAILURE_REASON)
        uut.scheduleNextRetry(XING_ID, attempts = 1, backoffDelaySeconds = 3600, DELETED_REASON)

        assertNull(scmOwnerRepository.findForUpdate(), "both seeded owners are deferred, so none is due")
    }

    @Test
    @Sql(SEED)
    fun `an owner becomes selectable again once its retry time has passed`() {
        uut.scheduleNextRetry(VOIZE_ID, attempts = 1, backoffDelaySeconds = 3600, FAILURE_REASON)
        // negative delay puts next_retry_at in the past, i.e. the backoff has elapsed
        uut.scheduleNextRetry(XING_ID, attempts = 1, backoffDelaySeconds = -3600, DELETED_REASON)

        val selected = assertNotNull(scmOwnerRepository.findForUpdate())
        assertEquals(XING_ID, selected.id, "only the owner whose backoff elapsed should be due")
    }
}
