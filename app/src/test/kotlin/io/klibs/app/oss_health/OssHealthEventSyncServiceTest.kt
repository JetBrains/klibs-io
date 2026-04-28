package io.klibs.app.oss_health

import io.klibs.app.configuration.properties.OssHealthProperties
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.health.entity.ScmRepoHealthComponentsEntity
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventEntity
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventType
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoIssueEventRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoIssueEventRepository.WindowAggregates
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssueEvent
import io.klibs.integration.github.model.GitHubIssueEventType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OssHealthEventSyncServiceTest {

    private val scmRepositoryRepository: ScmRepositoryRepository = mock()
    private val issueEventRepository: ScmRepoIssueEventRepository = mock()
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository = mock()
    private val gitHubIntegration: GitHubIntegration = mock()
    private val ossHealthProperties = OssHealthProperties()

    private val uut = OssHealthEventSyncService(
        scmRepositoryRepository,
        issueEventRepository,
        healthComponentsRepository,
        gitHubIntegration,
        ossHealthProperties,
    )

    private val now: Instant = Instant.parse("2026-04-29T12:00:00Z")
    private val windowStart: Instant = now.minus(12 * 7L, ChronoUnit.DAYS)
    private val pruneCutoff: Instant = now.minus(13 * 7L, ChronoUnit.DAYS)

    @Test
    fun `syncOldestRepo returns false and writes nothing when queue is empty`() {
        whenever(scmRepositoryRepository.findMultipleForHealthEventSync(limit = 1)).thenReturn(emptyList())

        val result = uut.syncOldestRepo()

        assertFalse(result)
        verifyNoInteractions(issueEventRepository, healthComponentsRepository, gitHubIntegration)
    }

    @Test
    fun `syncOldestRepo returns true when a repo is processed`() {
        val repo = repo(id = 7, nativeId = 700)
        whenever(scmRepositoryRepository.findMultipleForHealthEventSync(limit = 1)).thenReturn(listOf(repo))
        whenever(healthComponentsRepository.findById(7)).thenReturn(null)
        whenever(gitHubIntegration.recentIssueEvents(eq(700), any())).thenReturn(emptyList())
        whenever(issueEventRepository.aggregate(eq(7), any())).thenReturn(emptyAgg())

        assertTrue(uut.syncOldestRepo())
    }

    @Test
    fun `syncOne happy path writes events, prunes, aggregates, and advances timestamps in order`() {
        val repo = repo(id = 1, nativeId = 100)
        whenever(healthComponentsRepository.findById(1)).thenReturn(null)
        val events = listOf(
            event(
                GitHubIssueEventType.ISSUE,
                number = 42,
                createdAt = now.minus(20, ChronoUnit.DAYS),
                closedAt = now.minus(15, ChronoUnit.DAYS),
            ),
            event(
                GitHubIssueEventType.PR,
                number = 43,
                createdAt = now.minus(10, ChronoUnit.DAYS),
                mergedAt = now.minus(8, ChronoUnit.DAYS),
            ),
        )
        whenever(gitHubIntegration.recentIssueEvents(100, windowStart)).thenReturn(events)
        whenever(issueEventRepository.pruneOlderThan(1, pruneCutoff)).thenReturn(2)
        whenever(issueEventRepository.aggregate(1, windowStart))
            .thenReturn(WindowAggregates(5, 4, 6.0, 3, 2, 2.0))

        uut.syncOne(repo, now)

        val inOrder = inOrder(issueEventRepository, healthComponentsRepository)
        inOrder.verify(issueEventRepository).upsertAll(any())
        inOrder.verify(issueEventRepository).pruneOlderThan(1, pruneCutoff)
        inOrder.verify(issueEventRepository).aggregate(1, windowStart)
        inOrder.verify(healthComponentsRepository).upsertIssuePrComponents(
            scmRepoId = eq(1),
            issueOpenedCount = eq(5),
            issueClosedCount = eq(4),
            medianIssueCloseDays = eq(6.0),
            prOpenedCount = eq(3),
            prMergedCount = eq(2),
            medianPrMergeDays = eq(2.0),
            iScore = any(),
            pScore = any(),
        )
        inOrder.verify(healthComponentsRepository).setLastEventSyncTs(1, now)
        inOrder.verify(healthComponentsRepository).setNextHealthComputeTs(1, now)
    }

    @Test
    fun `syncOne with empty events list skips upsertAll but still prunes, aggregates, writes`() {
        val repo = repo(id = 2, nativeId = 200)
        whenever(healthComponentsRepository.findById(2)).thenReturn(null)
        whenever(gitHubIntegration.recentIssueEvents(200, windowStart)).thenReturn(emptyList())
        whenever(issueEventRepository.pruneOlderThan(2, pruneCutoff)).thenReturn(0)
        whenever(issueEventRepository.aggregate(2, windowStart)).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        verify(issueEventRepository, never()).upsertAll(any())
        verify(issueEventRepository).pruneOlderThan(2, pruneCutoff)
        verify(issueEventRepository).aggregate(2, windowStart)
        verify(healthComponentsRepository).setLastEventSyncTs(2, now)
        verify(healthComponentsRepository).setNextHealthComputeTs(2, now)
    }

    @Test
    fun `syncOne advances last_event_sync_ts and returns when GitHub fetch fails`() {
        val repo = repo(id = 3, nativeId = 300)
        whenever(healthComponentsRepository.findById(3)).thenReturn(null)
        whenever(gitHubIntegration.recentIssueEvents(300, windowStart))
            .thenThrow(RuntimeException("network blip"))

        uut.syncOne(repo, now)

        // Health repo: only findById (during pickSince) and setLastEventSyncTs.
        verify(healthComponentsRepository).findById(3)
        verify(healthComponentsRepository).setLastEventSyncTs(3, now)
        verifyNoMoreInteractions(healthComponentsRepository)
        // Event repo: never touched.
        verifyNoInteractions(issueEventRepository)
    }

    @Test
    fun `syncOne uses windowStart as since when no components row exists`() {
        val repo = repo(id = 4, nativeId = 400)
        whenever(healthComponentsRepository.findById(4)).thenReturn(null)
        whenever(gitHubIntegration.recentIssueEvents(400, windowStart)).thenReturn(emptyList())
        whenever(issueEventRepository.aggregate(eq(4), any())).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        verify(gitHubIntegration).recentIssueEvents(400, windowStart)
    }

    @Test
    fun `syncOne uses lastEventSyncTs minus 1h as since for recent sync`() {
        val repo = repo(id = 5, nativeId = 500)
        val lastSync = now.minus(6, ChronoUnit.DAYS)
        whenever(healthComponentsRepository.findById(5)).thenReturn(componentsWithLastSync(5, lastSync))
        whenever(gitHubIntegration.recentIssueEvents(eq(500), any())).thenReturn(emptyList())
        whenever(issueEventRepository.aggregate(eq(5), any())).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        verify(gitHubIntegration).recentIssueEvents(500, lastSync.minus(1, ChronoUnit.HOURS))
    }

    @Test
    fun `syncOne caps since at windowStart when lastEventSyncTs is older than 12 weeks`() {
        val repo = repo(id = 6, nativeId = 600)
        val veryOld = now.minus(13 * 7L, ChronoUnit.DAYS)
        whenever(healthComponentsRepository.findById(6)).thenReturn(componentsWithLastSync(6, veryOld))
        whenever(gitHubIntegration.recentIssueEvents(eq(600), any())).thenReturn(emptyList())
        whenever(issueEventRepository.aggregate(eq(6), any())).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        verify(gitHubIntegration).recentIssueEvents(600, windowStart)
    }

    @Test
    fun `syncOne writes null medians and null I and P when aggregate has no closed items`() {
        val repo = repo(id = 8, nativeId = 800)
        whenever(healthComponentsRepository.findById(8)).thenReturn(null)
        whenever(gitHubIntegration.recentIssueEvents(800, windowStart)).thenReturn(emptyList())
        whenever(issueEventRepository.aggregate(8, windowStart)).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        verify(healthComponentsRepository).upsertIssuePrComponents(
            scmRepoId = eq(8),
            issueOpenedCount = eq(0),
            issueClosedCount = eq(0),
            medianIssueCloseDays = isNull(),
            prOpenedCount = eq(0),
            prMergedCount = eq(0),
            medianPrMergeDays = isNull(),
            iScore = isNull(),
            pScore = isNull(),
        )
    }

    @Test
    fun `syncOne maps a merged PR event to an entity with non-negative duration`() {
        val repo = repo(id = 9, nativeId = 900)
        whenever(healthComponentsRepository.findById(9)).thenReturn(null)
        val createdAt = now.minus(10, ChronoUnit.DAYS)
        val mergedAt = now.minus(7, ChronoUnit.DAYS)
        val pr = event(GitHubIssueEventType.PR, number = 1, createdAt = createdAt, mergedAt = mergedAt)
        whenever(gitHubIntegration.recentIssueEvents(900, windowStart)).thenReturn(listOf(pr))
        whenever(issueEventRepository.aggregate(eq(9), any())).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        val captor = argumentCaptor<List<ScmRepoIssueEventEntity>>()
        verify(issueEventRepository).upsertAll(captor.capture())
        val mapped = captor.firstValue.single()
        assertEquals(9, mapped.scmRepoId)
        assertEquals(1, mapped.ghNumber)
        assertEquals(ScmRepoIssueEventType.PR, mapped.type)
        assertEquals(createdAt, mapped.createdAt)
        assertEquals(mergedAt, mapped.mergedAt)
        assertEquals(3, mapped.durationDays)
    }

    @Test
    fun `syncOne maps an open issue to an entity with null duration`() {
        val repo = repo(id = 10, nativeId = 1000)
        whenever(healthComponentsRepository.findById(10)).thenReturn(null)
        val open = event(
            GitHubIssueEventType.ISSUE,
            number = 5,
            createdAt = now.minus(5, ChronoUnit.DAYS),
            closedAt = null,
        )
        whenever(gitHubIntegration.recentIssueEvents(1000, windowStart)).thenReturn(listOf(open))
        whenever(issueEventRepository.aggregate(eq(10), any())).thenReturn(emptyAgg())

        uut.syncOne(repo, now)

        val captor = argumentCaptor<List<ScmRepoIssueEventEntity>>()
        verify(issueEventRepository).upsertAll(captor.capture())
        val mapped = captor.firstValue.single()
        assertEquals(ScmRepoIssueEventType.ISSUE, mapped.type)
        assertNull(mapped.closedAt)
        assertNull(mapped.mergedAt)
        assertNull(mapped.durationDays)
    }

    private fun repo(id: Int, nativeId: Long, owner: String = "octo", name: String = "repo") =
        ScmRepositoryEntity(
            id = id,
            nativeId = nativeId,
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

    private fun event(
        type: GitHubIssueEventType,
        number: Int,
        createdAt: Instant = now.minus(5, ChronoUnit.DAYS),
        closedAt: Instant? = null,
        mergedAt: Instant? = null,
        updatedAt: Instant = now,
    ) = GitHubIssueEvent(type, number, createdAt, closedAt, mergedAt, updatedAt)

    private fun emptyAgg() = WindowAggregates(0, 0, null, 0, 0, null)

    private fun componentsWithLastSync(id: Int, ts: Instant) = ScmRepoHealthComponentsEntity(
        scmRepoId = id, scoreRecomputedTs = null,
        issueOpenedCount = null, issueClosedCount = null, medianIssueCloseDays = null,
        prOpenedCount = null, prMergedCount = null, medianPrMergeDays = null,
        commitsCv = null, activeContributors = null, topContributorShare = null,
        cScore = null, iScore = null, pScore = null, aScore = null,
        healthScore = null,
        lastEventSyncTs = ts, nextHealthComputeTs = null,
    )
}
