package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.ScmRepositorySchedulingRepository
import io.klibs.core.scm.repository.health.OssHealthIssueOrPrSyncService
import io.klibs.core.scm.repository.health.OssHealthScoreService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit-level coverage for the merged repo-update loop: the regular update runs unconditionally,
 * each OSS sub-step is gated by `isDue`, and a sub-step failure does not block the next sub-step
 * or the next repo.
 */
class GitHubRepositoryUpdatingServiceTest {

    private val scmRepositoryRepository = mockk<ScmRepositoryRepository>()
    private val schedulingRepository = mockk<ScmRepositorySchedulingRepository>(relaxed = true)
    private val githubIndexingService = mockk<GitHubIndexingService>(relaxed = true)
    private val ossHealthIssueOrPrSyncService = mockk<OssHealthIssueOrPrSyncService>(relaxUnitFun = true)
    private val ossHealthScoreService = mockk<OssHealthScoreService>(relaxUnitFun = true)

    private val uut = GitHubRepositoryUpdatingService(
        scmRepositoryRepository = scmRepositoryRepository,
        schedulingRepository = schedulingRepository,
        githubIndexingService = githubIndexingService,
        ossHealthIssueOrPrSyncService = ossHealthIssueOrPrSyncService,
        ossHealthScoreService = ossHealthScoreService,
        reposUpdatedPerCall = 3,
    )

    @Test
    fun `skips both OSS sub-steps when neither is due`() {
        val repo = repo(id = 1)
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns listOf(repo)
        every { ossHealthIssueOrPrSyncService.isDue(repo, any()) } returns false
        every { ossHealthScoreService.isDue(repo, any()) } returns false

        uut.syncRepositoryWithGitHub()

        verify(exactly = 1) { githubIndexingService.updateRepo(repo) }
        verify(exactly = 1) { schedulingRepository.clearSchedule(repo.idNotNull) }
        verify(exactly = 0) { ossHealthIssueOrPrSyncService.syncOne(any(), any()) }
        verify(exactly = 0) { ossHealthScoreService.computeOne(any(), any()) }
    }

    @Test
    fun `runs both OSS sub-steps when both are due`() {
        val repo = repo(id = 1)
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns listOf(repo)
        every { ossHealthIssueOrPrSyncService.isDue(repo, any()) } returns true
        every { ossHealthScoreService.isDue(repo, any()) } returns true

        uut.syncRepositoryWithGitHub()

        verify(exactly = 1) { githubIndexingService.updateRepo(repo) }
        verify(exactly = 1) { ossHealthIssueOrPrSyncService.syncOne(repo, any()) }
        verify(exactly = 1) { ossHealthScoreService.computeOne(repo, any()) }
    }

    @Test
    fun `records failure and skips both OSS sub-steps when updateRepo throws`() {
        val repo = repo(id = 1)
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns listOf(repo)
        every { githubIndexingService.updateRepo(repo) } throws RuntimeException("boom")

        uut.syncRepositoryWithGitHub()

        verify(exactly = 1) { schedulingRepository.scheduleNextRetry(repo.idNotNull, any(), any()) }
        verify(exactly = 0) { schedulingRepository.clearSchedule(any()) }
        verify(exactly = 0) { ossHealthIssueOrPrSyncService.isDue(any(), any()) }
        verify(exactly = 0) { ossHealthScoreService.isDue(any(), any()) }
    }

    @Test
    fun `score sub-step still runs when issue-or-pr sync throws`() {
        val repo = repo(id = 1)
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns listOf(repo)
        every { ossHealthIssueOrPrSyncService.isDue(repo, any()) } returns true
        every { ossHealthScoreService.isDue(repo, any()) } returns true
        every { githubIndexingService.updateRepo(repo) } answers { firstArg<ScmRepositoryEntity>() }

        uut.syncRepositoryWithGitHub()

        verify(exactly = 1) { ossHealthScoreService.computeOne(repo, any()) }
    }

    @Test
    fun `next repo is processed when previous sub-step throws`() {
        val r1 = repo(id = 1)
        val r2 = repo(id = 2)
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns listOf(r1, r2)
        every { ossHealthScoreService.isDue(r1, any()) } returns true
        every { ossHealthScoreService.isDue(r2, any()) } returns false
        every { ossHealthIssueOrPrSyncService.isDue(any(), any()) } returns false
        every { githubIndexingService.updateRepo(any()) } answers { firstArg<ScmRepositoryEntity>() }

        uut.syncRepositoryWithGitHub()

        verify(exactly = 1) { githubIndexingService.updateRepo(r1) }
        verify(exactly = 1) { githubIndexingService.updateRepo(r2) }
    }

    @Test
    fun `does nothing when no repo is selected for update`() {
        every { scmRepositoryRepository.findMultipleForUpdate(any()) } returns emptyList()

        uut.syncRepositoryWithGitHub()

        verify(exactly = 0) { githubIndexingService.updateRepo(any()) }
        verify(exactly = 0) { schedulingRepository.clearSchedule(any()) }
        verify(exactly = 0) { schedulingRepository.scheduleNextRetry(any(), any(), any()) }
    }

    private fun repo(id: Int) = ScmRepositoryEntity(
        id = id,
        nativeId = id * 1000L,
        name = "repo-$id",
        description = null,
        defaultBranch = "main",
        createdTs = Instant.EPOCH,
        ownerId = 100,
        ownerType = ScmOwnerType.ORGANIZATION,
        ownerLogin = "owner-$id",
        homepage = null,
        hasGhPages = false,
        hasIssues = true,
        hasWiki = false,
        archived = false,
        hasReadme = false,
        licenseKey = null,
        licenseName = null,
        stars = 0,
        openIssues = 0,
        lastActivityTs = Instant.EPOCH,
        updatedAtTs = Instant.EPOCH,
    )
}
