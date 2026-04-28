package io.klibs.core.scm.repository.health.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueOrPrEntity
import io.klibs.core.scm.repository.health.enums.ScmRepoIssueOrPrType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration tests for [ScmRepoIssueOrPrRepository] against a real Testcontainer Postgres.
 * Exercises the JPA-side upsert/prune/find queries — particularly that the `findActiveSince`
 * predicate selects exactly the rows that contribute to the 12-week window metrics.
 * In-memory aggregation lives in [io.klibs.app.oss_health.OssHealthIssueOrPrSyncService] and is
 * covered by its own unit tests.
 */
class ScmRepoIssueOrPrRepositoryJdbcTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repo: ScmRepoIssueOrPrRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val windowStart: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)

    @Test
    @Sql("classpath:sql/ScmRepoIssueOrPrRepositoryJdbcTest/seed-repo.sql")
    fun `upsert updates in place when called with the same gh_number`() {
        upsertAll(listOf(issue(number = 7, createdDaysAgo = 10, closedDaysAgo = null)))
        // Now close it: same key (scm_repo_id, gh_number = 7) should update instead of inserting.
        upsertAll(listOf(issue(number = 7, createdDaysAgo = 10, closedDaysAgo = 2)))

        val rows = repo.findActiveSince(SCM_REPO_ID, windowStart)
        assertEquals(1, rows.size)
        assertEquals(7, rows.single().ghNumber)
        assertNotNull(rows.single().closedAt)
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueOrPrRepositoryJdbcTest/seed-repo.sql")
    fun `pruneOlderThan keeps rows whose closed_at is still inside the cutoff`() {
        // 1: both created_at and closed_at older than cutoff → pruned.
        // 2: created_at older than cutoff but closed_at recent → kept.
        // 3: created_at older than cutoff and never closed → pruned.
        // 4: created_at and closed_at both inside cutoff → kept.
        val cutoff = now.minus(13 * 7L, ChronoUnit.DAYS)
        upsertAll(listOf(
            issue(number = 1, createdDaysAgo = 100, closedDaysAgo = 95),
            issue(number = 2, createdDaysAgo = 100, closedDaysAgo = 30),
            issue(number = 3, createdDaysAgo = 100, closedDaysAgo = null),
            issue(number = 4, createdDaysAgo = 5, closedDaysAgo = 1),
        ))

        val pruned = repo.pruneOlderThan(SCM_REPO_ID, cutoff)
        assertEquals(2, pruned)

        // Verify which two survived by querying.
        val survivingNumbers = jdbcClient.sql("SELECT gh_number FROM scm_repo_issue_or_pr WHERE scm_repo_id = :id ORDER BY gh_number")
            .param("id", SCM_REPO_ID)
            .query(Int::class.java)
            .list()
        assertEquals(listOf(2, 4), survivingNumbers)
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueOrPrRepositoryJdbcTest/seed-repo.sql")
    fun `findActiveSince returns every row that contributes to the window`() {
        // All five fall inside the 12-week window — created, closed, or merged dates ≤ 30 days ago.
        upsertAll(listOf(
            issue(number = 10, createdDaysAgo = 30, closedDaysAgo = 26, durationDays = 4),
            issue(number = 11, createdDaysAgo = 20, closedDaysAgo = 12, durationDays = 8),
            issue(number = 12, createdDaysAgo = 5, closedDaysAgo = null),
            pr(number = 20, createdDaysAgo = 10, mergedDaysAgo = 9, durationDays = 1),
            pr(number = 21, createdDaysAgo = 15, mergedDaysAgo = 10, durationDays = 5),
        ))

        val rows = repo.findActiveSince(SCM_REPO_ID, windowStart)
        assertEquals(setOf(10, 11, 12, 20, 21), rows.map { it.ghNumber }.toSet())
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueOrPrRepositoryJdbcTest/seed-repo.sql")
    fun `findActiveSince excludes rows whose created, closed, and merged dates are all before windowStart`() {
        // 30: created in window, closed in window → returned.
        // 31: created and closed both before windowStart → excluded.
        // 32: created before windowStart but closed inside → returned (contributes to closed count).
        upsertAll(listOf(
            issue(number = 30, createdDaysAgo = 10, closedDaysAgo = 5, durationDays = 5),
            issue(number = 31, createdDaysAgo = 100, closedDaysAgo = 95, durationDays = 5),
            issue(number = 32, createdDaysAgo = 100, closedDaysAgo = 10, durationDays = 90),
        ))

        val rows = repo.findActiveSince(SCM_REPO_ID, windowStart)
        val numbers = rows.map { it.ghNumber }.toSet()
        assertEquals(setOf(30, 32), numbers)
        assertTrue(31 !in numbers)
    }

    /** Test-local convenience for seeding multiple rows in one shot. */
    private fun upsertAll(entities: List<ScmRepoIssueOrPrEntity>) {
        entities.forEach { e ->
            repo.upsert(
                scmRepoId = e.scmRepoId,
                ghNumber = e.ghNumber,
                type = e.type,
                createdAt = e.createdAt,
                closedAt = e.closedAt,
                mergedAt = e.mergedAt,
                durationDays = e.durationDays,
            )
        }
    }

    private fun issue(
        number: Int,
        createdDaysAgo: Long,
        closedDaysAgo: Long?,
        durationDays: Int? = closedDaysAgo?.let { (createdDaysAgo - it).toInt() },
    ) = ScmRepoIssueOrPrEntity(
        scmRepoId = SCM_REPO_ID,
        ghNumber = number,
        type = ScmRepoIssueOrPrType.ISSUE,
        createdAt = now.minus(createdDaysAgo, ChronoUnit.DAYS),
        closedAt = closedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        mergedAt = null,
        durationDays = durationDays,
    )

    private fun pr(
        number: Int,
        createdDaysAgo: Long,
        mergedDaysAgo: Long?,
        durationDays: Int? = mergedDaysAgo?.let { (createdDaysAgo - it).toInt() },
    ) = ScmRepoIssueOrPrEntity(
        scmRepoId = SCM_REPO_ID,
        ghNumber = number,
        type = ScmRepoIssueOrPrType.PR,
        createdAt = now.minus(createdDaysAgo, ChronoUnit.DAYS),
        closedAt = mergedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        mergedAt = mergedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        durationDays = durationDays,
    )

    companion object {
        // Match the IDs in seed-repo.sql.
        private const val SCM_REPO_ID = 700002
    }
}
