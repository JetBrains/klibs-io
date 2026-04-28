package io.klibs.app.oss_health

import BaseUnitWithDbLayerTest
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubCommitAuthor
import io.klibs.integration.github.model.GitHubParticipationStats
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end integration tests against Testcontainer Postgres for [OssHealthScoreService].
 * Mocks only [GitHubIntegration]; the score component repository is the real JPA repository.
 * Each test seeds a single scm_repo and reads the resulting `scm_repo_health_components` row
 * after the service runs.
 */
class OssHealthScoreServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: OssHealthScoreService

    @Autowired
    private lateinit var healthComponentsRepository: ScmRepoHealthComponentsRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val twelveWeeksAgo: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)

    @Test
    fun `computeOldestRepo returns false when no repo is queued`() {
        // No scm_repo + no components row → queue empty.
        assertFalse(uut.computeOldestRepo())
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOldestRepo returns true when a repo's next_health_compute_ts is due`() {
        val repo = seededRepo()
        // Make this repo eligible for the score queue.
        healthComponentsRepository.setNextHealthComputeTs(repo.idNotNull, now.minus(1, ChronoUnit.HOURS))
        // Simplest path: participation null → service defers and returns true.
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId)).thenReturn(null)

        assertTrue(uut.computeOldestRepo())
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne defers next_health_compute_ts by 2 minutes when participation is null`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId)).thenReturn(null)

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(now.plus(2, ChronoUnit.MINUTES), components.nextHealthComputeTs)
        // No score columns written.
        assertNull(components.cScore)
        assertNull(components.aScore)
        assertNull(components.healthScore)
        // GraphQL is never called when participation is null.
        verify(gitHubIntegration, org.mockito.kotlin.never()).getCommitAuthorCounts(any(), any(), any())
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne defers next_health_compute_ts by 1 hour when author counts are null`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo)).thenReturn(null)

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(now.plus(1, ChronoUnit.HOURS), components.nextHealthComputeTs)
        assertNull(components.cScore)
        assertNull(components.aScore)
        assertNull(components.healthScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne happy path writes all score columns and schedules a 7-day recompute`() {
        val repo = seededRepo()
        // Seed I/P side first so composeScore has all four sub-scores.
        healthComponentsRepository.upsertIssuePrComponents(
            scmRepoId = repo.idNotNull,
            issueOpenedCount = 5, issueClosedCount = 4, medianIssueCloseDays = 6.0,
            prOpenedCount = 3, prMergedCount = 2, medianPrMergeDays = 2.0,
            iScore = 0.7, pScore = 0.6,
        )

        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(listOf(
                GitHubCommitAuthor("alice", 8),
                GitHubCommitAuthor("bob", 7),
                GitHubCommitAuthor("carol", 5),
            ))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(now, components.scoreRecomputedTs)
        assertEquals(3, components.activeContributors)
        // 8/(8+7+5) = 8/20 = 0.4
        assertNotNull(components.topContributorShare)
        assertEquals(0.4, components.topContributorShare!!, 1e-9)
        assertNotNull(components.cScore)
        assertNotNull(components.aScore)
        assertNotNull(components.healthScore)
        assertEquals(now.plus(7, ChronoUnit.DAYS), components.nextHealthComputeTs)
        // I/P side untouched by the score upsert.
        assertEquals(5, components.issueOpenedCount)
        assertEquals(0.7, components.iScore)
        assertEquals(0.6, components.pScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes null healthScore when no prior I or P components exist`() {
        val repo = seededRepo()
        // No upsertIssuePrComponents call → I and P remain null → composeScore returns null.
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 5)))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(1, components.activeContributors)
        assertNotNull(components.cScore)
        // healthScore = null because composeScore returns null when any sub-score is null.
        assertNull(components.healthScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes topContributorShare equal to 1_0 when a single author owns all commits`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 30)))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(1, components.activeContributors)
        assertEquals(1.0, components.topContributorShare)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes zero activeContributors and null topShare when author counts are empty`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(emptyList())

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(0, components.activeContributors)
        assertNull(components.topContributorShare)
        assertNull(components.aScore)
        assertNull(components.healthScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes null cScore when every week has zero commits`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 0 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 1)))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertNull(components.cScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne calls getCommitAuthorCounts with now minus 12 weeks as the cutoff`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 1 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(eq(repo.ownerLogin), eq(repo.name), any())).thenReturn(emptyList())

        uut.computeOne(repo, now)

        val sinceCaptor = argumentCaptor<Instant>()
        verify(gitHubIntegration).getCommitAuthorCounts(eq(repo.ownerLogin), eq(repo.name), sinceCaptor.capture())
        assertEquals(twelveWeeksAgo, sinceCaptor.firstValue)
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

    companion object {
        private const val SEEDED_REPO_ID = 700002
        private const val SEEDED_REPO_NATIVE_ID = 700000002L
        private const val SEEDED_OWNER_ID = 700001
    }
}
