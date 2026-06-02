package io.klibs.app.job

import io.klibs.app.indexing.GitHubIndexingService
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.ScmRepositorySchedulingRepository
import io.klibs.core.scm.repository.health.OssHealthIssueOrPrSyncService
import io.klibs.core.scm.repository.health.OssHealthScoreService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit-level coverage for the merged repo-update loop: the regular update runs unconditionally,
 * each OSS sub-step is gated by `isDue`, and a sub-step failure does not block the next sub-step
 * or the next repo.
 */
class GitHubRepositoryUpdatingServiceTest {

    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val schedulingRepository: ScmRepositorySchedulingRepository = mock()
    private val githubIndexingService: GitHubIndexingService = mock()
    private val ossHealthIssueOrPrSyncService: OssHealthIssueOrPrSyncService = mock()
    private val ossHealthScoreService: OssHealthScoreService = mock()

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
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(listOf(repo))
        whenever(ossHealthIssueOrPrSyncService.isDue(eq(repo), any())).thenReturn(false)
        whenever(ossHealthScoreService.isDue(eq(repo), any())).thenReturn(false)

        uut.syncRepositoryWithGitHub()

        verify(githubIndexingService).updateRepo(repo)
        verify(schedulingRepository).clearSchedule(repo.idNotNull)
        verify(ossHealthIssueOrPrSyncService, never()).syncOne(any(), any())
        verify(ossHealthScoreService, never()).computeOne(any(), any())
    }

    @Test
    fun `runs both OSS sub-steps when both are due`() {
        val repo = repo(id = 1)
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(listOf(repo))
        whenever(ossHealthIssueOrPrSyncService.isDue(eq(repo), any())).thenReturn(true)
        whenever(ossHealthScoreService.isDue(eq(repo), any())).thenReturn(true)

        uut.syncRepositoryWithGitHub()

        verify(githubIndexingService).updateRepo(repo)
        verify(ossHealthIssueOrPrSyncService).syncOne(eq(repo), any())
        verify(ossHealthScoreService).computeOne(eq(repo), any())
    }

    @Test
    fun `records failure and skips both OSS sub-steps when updateRepo throws`() {
        val repo = repo(id = 1)
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(listOf(repo))
        whenever(githubIndexingService.updateRepo(repo)).thenThrow(RuntimeException("boom"))

        uut.syncRepositoryWithGitHub()

        verify(schedulingRepository).scheduleNextRetry(eq(repo.idNotNull), any(), any())
        verify(schedulingRepository, never()).clearSchedule(any())
        verify(ossHealthIssueOrPrSyncService, never()).isDue(any(), any())
        verify(ossHealthScoreService, never()).isDue(any(), any())
    }

    @Test
    fun `score sub-step still runs when issue-or-pr sync throws`() {
        val repo = repo(id = 1)
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(listOf(repo))
        whenever(ossHealthIssueOrPrSyncService.isDue(eq(repo), any())).thenReturn(true)
        whenever(ossHealthScoreService.isDue(eq(repo), any())).thenReturn(true)
        whenever(ossHealthIssueOrPrSyncService.syncOne(eq(repo), any())).thenThrow(RuntimeException("sync boom"))

        uut.syncRepositoryWithGitHub()

        verify(ossHealthScoreService).computeOne(eq(repo), any())
    }

    @Test
    fun `next repo is processed when previous sub-step throws`() {
        val r1 = repo(id = 1)
        val r2 = repo(id = 2)
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(listOf(r1, r2))
        whenever(ossHealthScoreService.isDue(eq(r1), any())).thenReturn(true)
        whenever(ossHealthScoreService.isDue(eq(r2), any())).thenReturn(false)
        whenever(ossHealthIssueOrPrSyncService.isDue(any(), any())).thenReturn(false)
        whenever(ossHealthScoreService.computeOne(eq(r1), any())).thenThrow(RuntimeException("score boom"))

        uut.syncRepositoryWithGitHub()

        verify(githubIndexingService).updateRepo(r1)
        verify(githubIndexingService).updateRepo(r2)
    }

    @Test
    fun `does nothing when no repo is selected for update`() {
        whenever(scmRepositoryRepository.findMultipleForUpdate(any())).thenReturn(emptyList())

        uut.syncRepositoryWithGitHub()

        verify(githubIndexingService, never()).updateRepo(any())
        verify(schedulingRepository, never()).clearSchedule(any())
        verify(schedulingRepository, never()).scheduleNextRetry(any(), any(), any())
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
        hasReadme = false,
        licenseKey = null,
        licenseName = null,
        stars = 0,
        openIssues = 0,
        lastActivityTs = Instant.EPOCH,
        updatedAtTs = Instant.EPOCH,
    )
}
