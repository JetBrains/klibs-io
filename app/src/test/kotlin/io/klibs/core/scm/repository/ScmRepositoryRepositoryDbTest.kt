package io.klibs.core.scm.repository

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for [ScmRepositoryRepository.findMultipleForUpdate] and the per-repo backoff
 * helpers against a Testcontainer Postgres. Seeds repos A–F (see seed-repos.sql); eligible ones,
 * highest-star first, are D(60), A(50), B(30), F(20). C is in an active backoff window and E was
 * updated within 24h.
 */
class ScmRepositoryRepositoryDbTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repo: ScmRepositoryRepository

    @Autowired
    private lateinit var scheduling: ScmRepositorySchedulingRepository

    @Test
    @Sql("classpath:sql/ScmRepositoryRepositoryDbTest/seed-repos.sql")
    fun `findMultipleForUpdate skips backed-off and fresh repos, ordered by stars desc`() {
        val ids = repo.findMultipleForUpdate(10).map { it.idNotNull }

        assertEquals(listOf(800013, 800010, 800011, 800015), ids) // D, A, B, F by stars desc
    }

    @Test
    @Sql("classpath:sql/ScmRepositoryRepositoryDbTest/seed-repos.sql")
    fun `scheduleNextRetry backs the repo off out of the next selection`() {
        assertTrue(repo.findMultipleForUpdate(10).any { it.idNotNull == 800010 })

        scheduling.scheduleNextRetry(800010, attempts = 1, backoffDelaySeconds = 3600) // next_retry_at an hour ahead

        assertFalse(repo.findMultipleForUpdate(10).any { it.idNotNull == 800010 })
    }

    @Test
    @Sql("classpath:sql/ScmRepositoryRepositoryDbTest/seed-repos.sql")
    fun `clearSchedule clears backoff so a backed-off repo is selectable again`() {
        // C (800012) is in an active backoff window (next_retry_at in the future).
        assertFalse(repo.findMultipleForUpdate(10).any { it.idNotNull == 800012 })

        scheduling.clearSchedule(800012) // removes the scheduling row

        assertTrue(repo.findMultipleForUpdate(10).any { it.idNotNull == 800012 })
    }
}
