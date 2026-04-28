package io.klibs.app.oss_health

import io.klibs.app.configuration.properties.OssHealthProperties
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.health.entity.ScmRepoHealthComponentsEntity
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubCommitAuthor
import io.klibs.integration.github.model.GitHubParticipationStats
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OssHealthScoreServiceTest {

    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository = mock()
    private val gitHubIntegration: GitHubIntegration = mock()
    private val ossHealthProperties = OssHealthProperties()

    private val uut = OssHealthScoreService(
        scmRepositoryRepository,
        healthComponentsRepository,
        gitHubIntegration,
        ossHealthProperties,
    )

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val twelveWeeksAgo: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)

    @Test
    fun `computeOldestRepo returns false when queue is empty`() {
        whenever(scmRepositoryRepository.findMultipleForHealthScoreCompute(limit = 1)).thenReturn(emptyList())

        val result = uut.computeOldestRepo()

        assertFalse(result)
        verifyNoInteractions(healthComponentsRepository, gitHubIntegration)
    }

    @Test
    fun `computeOldestRepo returns true when a repo is processed`() {
        val repo = repo(id = 1, owner = "octo", name = "repo")
        whenever(scmRepositoryRepository.findMultipleForHealthScoreCompute(limit = 1)).thenReturn(listOf(repo))
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId)).thenReturn(null)

        assertTrue(uut.computeOldestRepo())
    }

    @Test
    fun `computeOne defers 2 minutes when participation returns null and skips GraphQL`() {
        val repo = repo(id = 2)
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).setNextHealthComputeTs(2, now.plus(2, ChronoUnit.MINUTES))
        verifyNoMoreInteractions(healthComponentsRepository)
        verify(gitHubIntegration, never()).getCommitAuthorCounts(any(), any(), any())
    }

    @Test
    fun `computeOne defers 1 hour when author counts returns null`() {
        val repo = repo(id = 3, owner = "octo", name = "myrepo")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("octo", "myrepo", twelveWeeksAgo)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).setNextHealthComputeTs(3, now.plus(1, ChronoUnit.HOURS))
        verifyNoMoreInteractions(healthComponentsRepository)
    }

    @Test
    fun `computeOne happy path writes score components and schedules next recompute in order`() {
        val repo = repo(id = 4, owner = "octo", name = "good")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("octo", "good", twelveWeeksAgo))
            .thenReturn(listOf(
                GitHubCommitAuthor("alice", 8),
                GitHubCommitAuthor("bob", 7),
                GitHubCommitAuthor("carol", 5),
            ))
        whenever(healthComponentsRepository.findById(4))
            .thenReturn(componentsWithIp(4, iScore = 0.7, pScore = 0.6))

        uut.computeOne(repo, now)

        val inOrder = inOrder(healthComponentsRepository)
        inOrder.verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(4),
            scoreRecomputedTs = eq(now),
            commitsCv = any(),
            activeContributors = eq(3),
            topContributorShare = any(),
            cScore = any(),
            aScore = any(),
            healthScore = any(),
        )
        inOrder.verify(healthComponentsRepository).setNextHealthComputeTs(4, now.plus(7, ChronoUnit.DAYS))
    }

    @Test
    fun `computeOne writes null final score when components row missing leaves I and P null`() {
        val repo = repo(id = 5, owner = "x", name = "y")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("x", "y", twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 5)))
        whenever(healthComponentsRepository.findById(5)).thenReturn(null)

        uut.computeOne(repo, now)

        // composeScore returns null when any of C/I/P/A is null; here I and P are null.
        verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(5),
            scoreRecomputedTs = eq(now),
            commitsCv = any(),
            activeContributors = eq(1),
            topContributorShare = any(),
            cScore = any(),
            aScore = any(),
            healthScore = isNull(),
        )
    }

    @Test
    fun `computeOne writes activeContributors=0 and null topShare when author counts is empty`() {
        val repo = repo(id = 6, owner = "x", name = "z")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("x", "z", twelveWeeksAgo)).thenReturn(emptyList())
        whenever(healthComponentsRepository.findById(6)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(6),
            scoreRecomputedTs = eq(now),
            commitsCv = any(),
            activeContributors = eq(0),
            topContributorShare = isNull(),
            cScore = any(),
            aScore = isNull(),
            healthScore = isNull(),
        )
    }

    @Test
    fun `computeOne writes topShare=1_0 when one author owns all commits`() {
        val repo = repo(id = 7, owner = "x", name = "solo")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 5 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("x", "solo", twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 30)))
        whenever(healthComponentsRepository.findById(7)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(7),
            scoreRecomputedTs = eq(now),
            commitsCv = any(),
            activeContributors = eq(1),
            topContributorShare = eq(1.0),
            cScore = any(),
            aScore = any(),
            healthScore = isNull(), // I and P null (no components row) → composeScore null
        )
    }

    @Test
    fun `computeOne writes null cv and null C when participation has all-zero weeks`() {
        val repo = repo(id = 8, owner = "x", name = "dead")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(52) { 0 }))
        whenever(gitHubIntegration.getCommitAuthorCounts("x", "dead", twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 1)))
        whenever(healthComponentsRepository.findById(8)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(8),
            scoreRecomputedTs = eq(now),
            commitsCv = isNull(),
            activeContributors = eq(1),
            topContributorShare = any(),
            cScore = isNull(),
            aScore = any(),
            healthScore = isNull(),
        )
    }

    @Test
    fun `computeOne writes a numerically-correct cv for non-zero variance`() {
        val repo = repo(id = 9, owner = "x", name = "v")
        // 12 weeks: [10, 0, 10, 0, ...] alternating. Mean=5, variance=25, stddev=5, CV=1.0.
        val weeklyCommits = List(12) { if (it % 2 == 0) 10 else 0 }
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = weeklyCommits))
        whenever(gitHubIntegration.getCommitAuthorCounts("x", "v", twelveWeeksAgo))
            .thenReturn(listOf(GitHubCommitAuthor("alice", 1)))
        whenever(healthComponentsRepository.findById(9)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(healthComponentsRepository).upsertScoreComponents(
            scmRepoId = eq(9),
            scoreRecomputedTs = eq(now),
            commitsCv = argThat { this != null && kotlin.math.abs(this!! - 1.0) < 1e-6 },
            activeContributors = any(),
            topContributorShare = any(),
            cScore = any(),
            aScore = any(),
            healthScore = isNull(), // I and P null (no components row) → composeScore null
        )
    }

    @Test
    fun `computeOne uses now minus 12 weeks as the GraphQL since`() {
        val repo = repo(id = 10, owner = "x", name = "t")
        whenever(gitHubIntegration.getParticipationStats(repo.nativeId))
            .thenReturn(GitHubParticipationStats(weeklyAllCommits = List(12) { 1 }))
        whenever(gitHubIntegration.getCommitAuthorCounts(eq("x"), eq("t"), any())).thenReturn(emptyList())
        whenever(healthComponentsRepository.findById(10)).thenReturn(null)

        uut.computeOne(repo, now)

        verify(gitHubIntegration).getCommitAuthorCounts("x", "t", twelveWeeksAgo)
    }

    private fun repo(id: Int, owner: String = "octo", name: String = "repo") =
        ScmRepositoryEntity(
            id = id,
            nativeId = id.toLong() * 100,
            name = name,
            description = null,
            defaultBranch = "main",
            createdTs = Instant.EPOCH,
            ownerId = 0,
            ownerType = ScmOwnerType.ORGANIZATION,
            ownerLogin = owner,
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            hasReadme = true,
            licenseKey = null,
            licenseName = null,
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.EPOCH,
            updatedAtTs = Instant.EPOCH,
        )

    private fun componentsWithIp(id: Int, iScore: Double?, pScore: Double?) = ScmRepoHealthComponentsEntity(
        scmRepoId = id, scoreRecomputedTs = null,
        issueOpenedCount = null, issueClosedCount = null, medianIssueCloseDays = null,
        prOpenedCount = null, prMergedCount = null, medianPrMergeDays = null,
        commitsCv = null, activeContributors = null, topContributorShare = null,
        cScore = null, iScore = iScore, pScore = pScore, aScore = null,
        healthScore = null,
        lastEventSyncTs = null, nextHealthComputeTs = null,
    )
}
