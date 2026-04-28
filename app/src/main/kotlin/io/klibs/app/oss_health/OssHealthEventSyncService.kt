package io.klibs.app.oss_health

import io.klibs.app.configuration.properties.OssHealthProperties
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventEntity
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueEventType
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoIssueEventRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssueEvent
import io.klibs.integration.github.model.GitHubIssueEventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Sliding-window maintainer for the OSS Health issue/PR event table.
 *
 * Each run per repo:
 * 1. Streams issues/PRs updated since `max(now - 12 weeks, last_event_sync_ts - 1h)`.
 * 2. Upserts them into `scm_repo_issue_event`.
 * 3. Prunes events whose `created_at` and `closed_at` are both older than 13 weeks.
 * 4. Recomputes I/P components from the stored events and writes them to
 *    `scm_repo_health_components` (C/A/final score are written by [OssHealthScoreService]).
 * 5. Advances `scm_repo.last_event_sync_ts` so the queue moves on.
 */
@Service
class OssHealthEventSyncService(
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val issueEventRepository: ScmRepoIssueEventRepository,
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository,
    private val gitHubIntegration: GitHubIntegration,
    private val ossHealthProperties: OssHealthProperties,
) {

    @Transactional
    fun syncOldestRepo(): Boolean {
        val repos = scmRepositoryRepository.findMultipleForHealthEventSync(limit = 1)
        val repo = repos.firstOrNull() ?: return false
        syncOne(repo, Instant.now())
        return true
    }

    fun syncOne(repo: ScmRepositoryEntity, now: Instant) {
        val windowStart = now.minus(WINDOW_WEEKS.toLong() * 7, ChronoUnit.DAYS)

        val lastSync = healthComponentsRepository.findById(repo.idNotNull)?.lastEventSyncTs
        val since = lastSync?.minus(1, ChronoUnit.HOURS)?.coerceAtLeast(windowStart) ?: windowStart

        logger.info(
            "Syncing OSS health events for repoId={} {}/{} since={}",
            repo.idNotNull, repo.ownerLogin, repo.name, since
        )

        val events = runCatching {
            gitHubIntegration.recentIssueEvents(repo.nativeId, since)
        }.onFailure { e ->
            logger.warn("Failed to fetch issue events for ${repo.ownerLogin}/${repo.name}: ${e.message}")
            healthComponentsRepository.setLastEventSyncTs(repo.idNotNull, now)
            return
        }.getOrThrow()

        if (events.isNotEmpty()) {
            issueEventRepository.upsertAll(events.map { it.toEntity(repo.idNotNull) })
            logger.debug("Upserted {} events for repoId={}", events.size, repo.idNotNull)
        }

        val pruneCutoff = now.minus((WINDOW_WEEKS + 1).toLong() * 7, ChronoUnit.DAYS)
        val pruned = issueEventRepository.pruneOlderThan(repo.idNotNull, pruneCutoff)
        if (pruned > 0) logger.debug("Pruned {} stale events for repoId={}", pruned, repo.idNotNull)

        val agg = issueEventRepository.aggregate(repo.idNotNull, windowStart)

        val i = OssHealthFormula.issueResponsiveness(
            opened = agg.issueOpenedCount,
            closed = agg.issueClosedCount,
            medianCloseDays = agg.medianIssueCloseDays,
            medianDaysThreshold = ossHealthProperties.issueMedianDaysThreshold,
        )
        val p = OssHealthFormula.prManagement(
            opened = agg.prOpenedCount,
            merged = agg.prMergedCount,
            medianMergeDays = agg.medianPrMergeDays,
            medianDaysThreshold = ossHealthProperties.prMedianDaysThreshold,
        )

        healthComponentsRepository.upsertIssuePrComponents(
            scmRepoId = repo.idNotNull,
            issueOpenedCount = agg.issueOpenedCount,
            issueClosedCount = agg.issueClosedCount,
            medianIssueCloseDays = agg.medianIssueCloseDays,
            prOpenedCount = agg.prOpenedCount,
            prMergedCount = agg.prMergedCount,
            medianPrMergeDays = agg.medianPrMergeDays,
            iScore = i,
            pScore = p,
        )

        healthComponentsRepository.setLastEventSyncTs(repo.idNotNull, now)

        // If we have never computed a score before, seed `next_health_compute_ts` to now
        // so the score job will pick this repo up on its next tick.
        healthComponentsRepository.setNextHealthComputeTs(repo.idNotNull, now)

        logger.info(
            "OSS health events synced for repoId={}: events={}, window issues o/c={}/{}, prs o/m={}/{}, " +
                    "medians i/p={}/{} days, I={}, P={}",
            repo.idNotNull, events.size,
            agg.issueOpenedCount, agg.issueClosedCount,
            agg.prOpenedCount, agg.prMergedCount,
            agg.medianIssueCloseDays, agg.medianPrMergeDays,
            i, p
        )
    }

    private fun GitHubIssueEvent.toEntity(repoId: Int): ScmRepoIssueEventEntity {
        val closingTs = when (type) {
            GitHubIssueEventType.PR -> mergedAt ?: closedAt
            GitHubIssueEventType.ISSUE -> closedAt
        }
        val duration: Int? = if (closingTs != null) {
            Duration.between(createdAt, closingTs).toDays().toInt().coerceAtLeast(0)
        } else null
        return ScmRepoIssueEventEntity(
            scmRepoId = repoId,
            ghNumber = number,
            type = when (type) {
                GitHubIssueEventType.ISSUE -> ScmRepoIssueEventType.ISSUE
                GitHubIssueEventType.PR -> ScmRepoIssueEventType.PR
            },
            createdAt = createdAt,
            closedAt = closedAt,
            mergedAt = mergedAt,
            durationDays = duration,
        )
    }

    companion object {
        private const val WINDOW_WEEKS = 12
        private val logger = LoggerFactory.getLogger(OssHealthEventSyncService::class.java)
    }
}
