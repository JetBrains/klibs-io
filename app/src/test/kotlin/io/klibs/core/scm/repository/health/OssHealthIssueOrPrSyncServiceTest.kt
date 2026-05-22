package io.klibs.core.scm.repository.health

import BaseUnitWithDbLayerTest
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.health.enums.ScmRepoIssueOrPrType
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoIssueOrPrRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.github.model.GitHubPullRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end integration tests against Testcontainer Postgres. Drives the real
 * [OssHealthIssueOrPrSyncService] against real JPA repositories; mocks only [GitHubIntegration],
 * the genuine external dependency. Assertions read the actual rows that landed in
 * `scm_repo_issue_or_pr` and `scm_repo_health_components`.
 */
class OssHealthIssueOrPrSyncServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: OssHealthIssueOrPrSyncService

    @Autowired
    private lateinit var issueOrPrRepository: ScmRepoIssueOrPrRepository

    @Autowired
    private lateinit var healthComponentsRepository: ScmRepoHealthComponentsRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val windowStart: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `isDue returns true when no components row exists and hasIssues is true`() {
        val repo = seededRepo()
        assertTrue(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `isDue returns false when hasIssues is false`() {
        val repo = seededRepo().copy(hasIssues = false)
        assertFalse(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `isDue returns false when last sync was less than 7 days ago`() {
        val repo = seededRepo()
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, now.minus(1, ChronoUnit.DAYS))
        assertFalse(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `isDue returns true when last sync was more than 7 days ago`() {
        val repo = seededRepo()
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, now.minus(8, ChronoUnit.DAYS))
        assertTrue(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne happy path writes rows and aggregated components row`() {
        val repo = seededRepo()
        // 5 issues opened in window: 4 closed with durations [2,4,8,10] (median 6.0), 1 open.
        // 3 PRs   opened in window: 2 merged with durations [1,3]    (median 2.0), 1 open.
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(
            listOf(
                githubIssue(number = 101, createdDaysAgo = 50, closedDaysAgo = 48),
                githubIssue(number = 102, createdDaysAgo = 40, closedDaysAgo = 36),
                githubIssue(number = 103, createdDaysAgo = 30, closedDaysAgo = 22),
                githubIssue(number = 104, createdDaysAgo = 20, closedDaysAgo = 10),
                githubIssue(number = 105, createdDaysAgo = 5, closedDaysAgo = null),
            )
        )
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(
            listOf(
                githubPr(number = 201, createdDaysAgo = 30, mergedDaysAgo = 29),
                githubPr(number = 202, createdDaysAgo = 20, mergedDaysAgo = 17),
                githubPr(number = 203, createdDaysAgo = 10, mergedDaysAgo = null),
            )
        )

        uut.syncOne(repo, now)

        // 8 rows landed in scm_repo_issue_or_pr.
        val stored = issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart)
        assertEquals(setOf(101, 102, 103, 104, 105, 201, 202, 203), stored.map { it.ghNumber }.toSet())

        // Aggregated components row matches the constructed window.
        val components = healthComponentsRepository.findByScmRepoId(repo.idNotNull)
        assertNotNull(components)
        assertEquals(5, components.issueOpenedCount)
        assertEquals(4, components.issueClosedCount)
        assertEquals(6.0, components.medianIssueCloseDays)
        assertEquals(3, components.prOpenedCount)
        assertEquals(2, components.prMergedCount)
        assertEquals(2.0, components.medianPrMergeDays)
        assertNotNull(components.iScore)
        assertNotNull(components.pScore)
        assertEquals(now, components.lastIssueOrPrSyncTs)
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne with no GitHub issues or PRs still writes a zero-count components row`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(emptyList())
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(emptyList())

        uut.syncOne(repo, now)

        assertEquals(0, issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart).size)

        val components = healthComponentsRepository.findByScmRepoId(repo.idNotNull)
        assertNotNull(components)
        assertEquals(0, components.issueOpenedCount)
        assertEquals(0, components.issueClosedCount)
        assertNull(components.medianIssueCloseDays)
        assertEquals(0, components.prOpenedCount)
        assertEquals(0, components.prMergedCount)
        assertNull(components.medianPrMergeDays)
        assertEquals(now, components.lastIssueOrPrSyncTs)
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne propagates and writes nothing when the GitHub call throws`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any()))
            .thenThrow(RuntimeException("network blip"))

        assertFailsWith<RuntimeException> { uut.syncOne(repo, now) }

        // No rows written, no components row created — retry happens on the next repo update tick.
        assertEquals(0, issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart).size)
        assertNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne uses windowStart as since when no prior sync exists`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(emptyList())
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(emptyList())

        uut.syncOne(repo, now)

        val sinceCaptor = argumentCaptor<Instant>()
        verify(gitHubIntegration).recentIssues(eq(repo.nativeId), sinceCaptor.capture())
        assertEquals(windowStart, sinceCaptor.firstValue)
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne uses lastIssueOrPrSyncTs minus 1h as since when last sync was recent`() {
        val repo = seededRepo()
        val lastSync = now.minus(6, ChronoUnit.DAYS)
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, lastSync)
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(emptyList())
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(emptyList())

        uut.syncOne(repo, now)

        val sinceCaptor = argumentCaptor<Instant>()
        verify(gitHubIntegration).recentIssues(eq(repo.nativeId), sinceCaptor.capture())
        assertEquals(lastSync.minus(1, ChronoUnit.HOURS), sinceCaptor.firstValue)
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne caps since at windowStart when last sync is older than the 12-week window`() {
        val repo = seededRepo()
        val veryOld = now.minus(13 * 7L, ChronoUnit.DAYS)
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, veryOld)
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(emptyList())
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(emptyList())

        uut.syncOne(repo, now)

        val sinceCaptor = argumentCaptor<Instant>()
        verify(gitHubIntegration).recentIssues(eq(repo.nativeId), sinceCaptor.capture())
        assertEquals(windowStart, sinceCaptor.firstValue)
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne is idempotent when run twice on the same GitHub issues and PRs`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(
            listOf(githubIssue(number = 1, createdDaysAgo = 10, closedDaysAgo = 5))
        )
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(
            listOf(githubPr(number = 2, createdDaysAgo = 10, mergedDaysAgo = 5))
        )

        uut.syncOne(repo, now)
        uut.syncOne(repo, now)

        // 2 unique rows, not 4 — upsert dedupes on (scm_repo_id, gh_number).
        val stored = issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart)
        assertEquals(setOf(1, 2), stored.map { it.ghNumber }.toSet())
    }

    @Test
    @Sql("classpath:sql/OssHealthIssueOrPrSyncServiceTest/seed-repo.sql")
    fun `syncOne prunes rows whose created and closed dates are both before the prune cutoff`() {
        val repo = seededRepo()
        // Seed the table directly with a row whose dates are both 100 days ago (pruneCutoff is 91 days).
        issueOrPrRepository.upsert(
            scmRepoId = repo.idNotNull,
            ghNumber = 999,
            type = ScmRepoIssueOrPrType.ISSUE,
            createdAt = now.minus(100, ChronoUnit.DAYS),
            closedAt = now.minus(95, ChronoUnit.DAYS),
            mergedAt = null,
            durationDays = 5,
        )
        whenever(gitHubIntegration.recentIssues(eq(repo.nativeId), any())).thenReturn(emptyList())
        whenever(gitHubIntegration.recentPrs(eq(repo.nativeId), any())).thenReturn(emptyList())

        uut.syncOne(repo, now)

        // The 100-day-old row should be pruned; nothing remains in the active window.
        val stored = issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart)
        assertEquals(0, stored.size)
    }

    private fun seededRepo() = ScmRepositoryEntity(
        id = SEEDED_REPO_ID,
        nativeId = SEEDED_REPO_NATIVE_ID,
        name = "oss-health-test-repo",
        description = null,
        defaultBranch = "main",
        createdTs = Instant.EPOCH,
        ownerId = SEEDED_OWNER_ID,
        ownerType = ScmOwnerType.ORGANIZATION,
        ownerLogin = "oss-health-test",
        homepage = null,
        hasGhPages = false,
        hasIssues = true,
        hasWiki = false,
        hasReadme = false,
        licenseKey = null,
        licenseName = null,
        stars = 0,
        openIssues = 0,
        lastActivityTs = Instant.EPOCH,
        updatedAtTs = Instant.EPOCH,
    )

    private fun githubIssue(
        number: Int,
        createdDaysAgo: Long,
        closedDaysAgo: Long?,
    ) = GitHubIssue(
        number = number,
        createdAt = now.minus(createdDaysAgo, ChronoUnit.DAYS),
        closedAt = closedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        updatedAt = now,
    )

    private fun githubPr(
        number: Int,
        createdDaysAgo: Long,
        mergedDaysAgo: Long?,
    ) = GitHubPullRequest(
        number = number,
        createdAt = now.minus(createdDaysAgo, ChronoUnit.DAYS),
        closedAt = mergedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        mergedAt = mergedDaysAgo?.let { now.minus(it, ChronoUnit.DAYS) },
        updatedAt = now,
    )

    companion object {
        private const val SEEDED_REPO_ID = 700002
        private const val SEEDED_REPO_NATIVE_ID = 700000002L
        private const val SEEDED_OWNER_ID = 700001
    }
}
