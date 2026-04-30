package io.klibs.core.scm.repository.health.repository

import BaseUnitWithDbLayerTest
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventEntity
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration tests for [ScmRepoIssueEventRepositoryJdbc] against a real Testcontainer Postgres.
 * The Postgres-specific aggregate (`percentile_cont` median, `COUNT(*) FILTER (...)`) cannot be
 * validated with mocks, so this is the only place that exercises it end-to-end.
 */
class ScmRepoIssueEventRepositoryJdbcTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repo: ScmRepoIssueEventRepositoryJdbc

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val windowStart: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)

    @Test
    @Sql("classpath:sql/ScmRepoIssueEventRepositoryJdbcTest/seed-repo.sql")
    fun `upsertAll updates in place when called with the same gh_number`() {
        repo.upsertAll(listOf(issue(number = 7, createdDaysAgo = 10, closedDaysAgo = null)))
        // Now close it: same key (scm_repo_id, gh_number = 7) should update instead of inserting.
        repo.upsertAll(listOf(issue(number = 7, createdDaysAgo = 10, closedDaysAgo = 2)))

        val agg = repo.aggregate(SCM_REPO_ID, windowStart)
        assertEquals(1, agg.issueOpenedCount)
        assertEquals(1, agg.issueClosedCount)
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueEventRepositoryJdbcTest/seed-repo.sql")
    fun `pruneOlderThan keeps rows whose closed_at is still inside the cutoff`() {
        // 1: both created_at and closed_at older than cutoff → pruned.
        // 2: created_at older than cutoff but closed_at recent → kept.
        // 3: created_at older than cutoff and never closed → pruned.
        // 4: created_at and closed_at both inside cutoff → kept.
        val cutoff = now.minus(13 * 7L, ChronoUnit.DAYS)
        repo.upsertAll(listOf(
            issue(number = 1, createdDaysAgo = 100, closedDaysAgo = 95),
            issue(number = 2, createdDaysAgo = 100, closedDaysAgo = 30),
            issue(number = 3, createdDaysAgo = 100, closedDaysAgo = null),
            issue(number = 4, createdDaysAgo = 5, closedDaysAgo = 1),
        ))

        val pruned = repo.pruneOlderThan(SCM_REPO_ID, cutoff)
        assertEquals(2, pruned)

        // Verify which two survived by querying.
        val survivingNumbers = jdbcClient.sql("SELECT gh_number FROM scm_repo_issue_event WHERE scm_repo_id = :id ORDER BY gh_number")
            .param("id", SCM_REPO_ID)
            .query(Int::class.java)
            .list()
        assertEquals(listOf(2, 4), survivingNumbers)
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueEventRepositoryJdbcTest/seed-repo.sql")
    fun `aggregate counts issues and PRs separately and computes medians via percentile_cont`() {
        // Issues: 3 opened in window, 2 closed (durations 4 and 8 → median 6).
        // PRs:    2 opened in window, 2 merged (durations 1 and 5 → median 3).
        repo.upsertAll(listOf(
            issue(number = 10, createdDaysAgo = 30, closedDaysAgo = 26, durationDays = 4),
            issue(number = 11, createdDaysAgo = 20, closedDaysAgo = 12, durationDays = 8),
            issue(number = 12, createdDaysAgo = 5, closedDaysAgo = null),
            pr(number = 20, createdDaysAgo = 10, mergedDaysAgo = 9, durationDays = 1),
            pr(number = 21, createdDaysAgo = 15, mergedDaysAgo = 10, durationDays = 5),
        ))

        val agg = repo.aggregate(SCM_REPO_ID, windowStart)
        assertEquals(3, agg.issueOpenedCount)
        assertEquals(2, agg.issueClosedCount)
        assertEquals(6.0, agg.medianIssueCloseDays)
        assertEquals(2, agg.prOpenedCount)
        assertEquals(2, agg.prMergedCount)
        assertEquals(3.0, agg.medianPrMergeDays)
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueEventRepositoryJdbcTest/seed-repo.sql")
    fun `aggregate excludes events with created_at or closed_at outside the window`() {
        // Inside window:
        repo.upsertAll(listOf(
            issue(number = 30, createdDaysAgo = 10, closedDaysAgo = 5, durationDays = 5),
        ))
        // Outside window — created and closed before windowStart:
        repo.upsertAll(listOf(
            issue(number = 31, createdDaysAgo = 100, closedDaysAgo = 95, durationDays = 5),
        ))
        // Created outside window, closed inside — counted in closed but not opened:
        repo.upsertAll(listOf(
            issue(number = 32, createdDaysAgo = 100, closedDaysAgo = 10, durationDays = 90),
        ))

        val agg = repo.aggregate(SCM_REPO_ID, windowStart)
        assertEquals(1, agg.issueOpenedCount) // only #30 was created within the window
        assertEquals(2, agg.issueClosedCount) // #30 and #32 were closed within the window
    }

    @Test
    @Sql("classpath:sql/ScmRepoIssueEventRepositoryJdbcTest/seed-repo.sql")
    fun `aggregate returns null medians when no closed or merged events fall in the window`() {
        repo.upsertAll(listOf(
            issue(number = 40, createdDaysAgo = 5, closedDaysAgo = null),
            pr(number = 50, createdDaysAgo = 5, mergedDaysAgo = null),
        ))

        val agg = repo.aggregate(SCM_REPO_ID, windowStart)
        assertEquals(1, agg.issueOpenedCount)
        assertEquals(0, agg.issueClosedCount)
        assertNull(agg.medianIssueCloseDays)
        assertEquals(1, agg.prOpenedCount)
        assertEquals(0, agg.prMergedCount)
        assertNull(agg.medianPrMergeDays)
    }

    private fun issue(
        number: Int,
        createdDaysAgo: Long,
        closedDaysAgo: Long?,
        durationDays: Int? = closedDaysAgo?.let { (createdDaysAgo - it).toInt() },
    ) = ScmRepoIssueEventEntity(
        scmRepoId = SCM_REPO_ID,
        ghNumber = number,
        type = ScmRepoIssueEventType.ISSUE,
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
    ) = ScmRepoIssueEventEntity(
        scmRepoId = SCM_REPO_ID,
        ghNumber = number,
        type = ScmRepoIssueEventType.PR,
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
