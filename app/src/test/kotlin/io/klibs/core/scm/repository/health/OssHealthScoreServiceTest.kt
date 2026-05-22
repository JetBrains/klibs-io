package io.klibs.core.scm.repository.health

import BaseUnitWithDbLayerTest
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.integration.github.GitHubIntegration
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
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `isDue returns false when no components row exists`() {
        // Issue/PR sync has never run for this repo, so there's no I/P data to compose a score from.
        assertFalse(uut.isDue(seededRepo(), now))
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `isDue returns false when issue-or-pr sync has not run yet`() {
        val repo = seededRepo()
        // Components row exists from some other write, but lastIssueOrPrSyncTs is still null.
        healthComponentsRepository.upsertScoreComponents(
            scmRepoId = repo.idNotNull,
            scoreRecomputedTs = now.minus(1, ChronoUnit.DAYS),
            activeContributors = null, topContributorShare = null,
            cScore = null, aScore = null, healthScore = null,
        )
        assertFalse(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `isDue returns true when issue-or-pr sync ran but score has not`() {
        val repo = seededRepo()
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, now)
        assertTrue(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `isDue returns false when score was recomputed at or after the last issue-or-pr sync`() {
        val repo = seededRepo()
        val syncTs = now.minus(1, ChronoUnit.DAYS)
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, syncTs)
        healthComponentsRepository.upsertScoreComponents(
            scmRepoId = repo.idNotNull,
            scoreRecomputedTs = syncTs,
            activeContributors = null, topContributorShare = null,
            cScore = null, aScore = null, healthScore = null,
        )
        assertFalse(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `isDue returns true when score is older than the last issue-or-pr sync`() {
        val repo = seededRepo()
        healthComponentsRepository.upsertScoreComponents(
            scmRepoId = repo.idNotNull,
            scoreRecomputedTs = now.minus(2, ChronoUnit.DAYS),
            activeContributors = null, topContributorShare = null,
            cScore = null, aScore = null, healthScore = null,
        )
        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, now.minus(1, ChronoUnit.DAYS))
        assertTrue(uut.isDue(repo, now))
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes nothing when getCommitsByWeek throws`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId))
            .thenThrow(RuntimeException("simulated 202 from /stats/participation"))

        uut.computeOne(repo, now)

        // No components row created; nothing to retry-schedule — next repo update tick re-checks.
        assertNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        // GraphQL is never called when the commits-by-week call fails.
        verify(gitHubIntegration, org.mockito.kotlin.never()).getCommitAuthorCounts(any(), any(), any())
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes nothing when getCommitAuthorCounts throws`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 5 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenThrow(RuntimeException("simulated GraphQL failure"))

        uut.computeOne(repo, now)

        assertNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
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

        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 5 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(mapOf("alice" to 8, "bob" to 7, "carol" to 5))

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
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 5 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(mapOf("alice" to 5))

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
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 5 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(mapOf("alice" to 30))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertEquals(1, components.activeContributors)
        assertEquals(1.0, components.topContributorShare)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne writes zero activeContributors and null topShare when author counts are empty`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 5 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(emptyMap())

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
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 0 })
        whenever(gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo))
            .thenReturn(mapOf("alice" to 1))

        uut.computeOne(repo, now)

        val components = assertNotNull(healthComponentsRepository.findByScmRepoId(repo.idNotNull))
        assertNull(components.cScore)
    }

    @Test
    @Sql("classpath:sql/OssHealthScoreServiceTest/seed-repo.sql")
    fun `computeOne calls getCommitAuthorCounts with now minus 12 weeks as the cutoff`() {
        val repo = seededRepo()
        whenever(gitHubIntegration.getCommitsByWeek(repo.nativeId)).thenReturn(List(52) { 1 })
        whenever(gitHubIntegration.getCommitAuthorCounts(eq(repo.ownerLogin), eq(repo.name), any())).thenReturn(emptyMap())

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
