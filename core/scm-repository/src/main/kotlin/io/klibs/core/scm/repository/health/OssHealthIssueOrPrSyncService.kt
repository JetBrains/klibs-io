package io.klibs.core.scm.repository.health

import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.health.entity.ScmRepoIssueOrPrEntity
import io.klibs.core.scm.repository.health.enums.ScmRepoIssueOrPrType
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoIssueOrPrRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.github.model.GitHubPullRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Sliding-window maintainer for the OSS Health issue/PR table.
 *
 * Each run per repo:
 * 1. Streams issues/PRs updated since `max(now - 12 weeks, last_issue_or_pr_sync_ts - 1h)`.
 * 2. Upserts them into `scm_repo_issue_or_pr`.
 * 3. Prunes rows whose `created_at` and `closed_at` are both older than 13 weeks.
 * 4. Recomputes I/P components from the stored rows and writes them to
 *    `scm_repo_health_components` (C/A/final score are written by [OssHealthScoreService]).
 * 5. Advances `scm_repo.last_issue_or_pr_sync_ts` so the queue moves on.
 */
@Service
class OssHealthIssueOrPrSyncService(
    private val issueOrPrRepository: ScmRepoIssueOrPrRepository,
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository,
    private val gitHubIntegration: GitHubIntegration,
    private val ossHealthProperties: OssHealthProperties,
) {

    /**
     * Whether [repo] needs an OSS issue/PR sync right now: it must have issues enabled and either
     * have never been synced or have been synced more than 7 days ago.
     */
    fun isDue(repo: ScmRepositoryEntity, now: Instant): Boolean {
        if (!repo.hasIssues) return false
        val lastSync = healthComponentsRepository.findByScmRepoId(repo.idNotNull)?.lastIssueOrPrSyncTs
            ?: return true
        return lastSync.isBefore(now.minus(SYNC_CADENCE_DAYS, ChronoUnit.DAYS))
    }

    @Transactional
    fun syncOne(repo: ScmRepositoryEntity, now: Instant) {
        val windowStart = now.minus(WINDOW_WEEKS.toLong() * 7, ChronoUnit.DAYS)

        val lastSync = healthComponentsRepository.findByScmRepoId(repo.idNotNull)?.lastIssueOrPrSyncTs
        val since = lastSync?.minus(1, ChronoUnit.HOURS)?.coerceAtLeast(windowStart) ?: windowStart

        logger.info(
            "Syncing OSS health issues/PRs for repoId={} {}/{} since={}",
            repo.idNotNull, repo.ownerLogin, repo.name, since
        )

        val fetchedIssues = gitHubIntegration.recentIssues(repo.nativeId, since)
        val fetchedPrs = gitHubIntegration.recentPrs(repo.nativeId, since)
        val fetchedCount = fetchedIssues.size + fetchedPrs.size

        if (fetchedCount > 0) {
            fetchedIssues.forEach { it.upsertInto(issueOrPrRepository, repo.idNotNull) }
            fetchedPrs.forEach { it.upsertInto(issueOrPrRepository, repo.idNotNull) }
            logger.debug(
                "Upserted {} issues and {} PRs for repoId={}",
                fetchedIssues.size, fetchedPrs.size, repo.idNotNull,
            )
        }

        val pruneCutoff = now.minus((WINDOW_WEEKS + 1).toLong() * 7, ChronoUnit.DAYS)
        val pruned = issueOrPrRepository.pruneOlderThan(repo.idNotNull, pruneCutoff)
        if (pruned > 0) logger.debug("Pruned {} stale issues/PRs for repoId={}", pruned, repo.idNotNull)

        val activeIssuesOrPrs = issueOrPrRepository.findActiveSince(repo.idNotNull, windowStart)
        val agg = aggregate(activeIssuesOrPrs, windowStart)

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

        healthComponentsRepository.setLastIssueOrPrSyncTs(repo.idNotNull, now)

        logger.info(
            "OSS health issues/PRs synced for repoId={}: fetched={}, window issues o/c={}/{}, prs o/m={}/{}, " +
                    "medians i/p={}/{} days, I={}, P={}",
            repo.idNotNull, fetchedCount,
            agg.issueOpenedCount, agg.issueClosedCount,
            agg.prOpenedCount, agg.prMergedCount,
            agg.medianIssueCloseDays, agg.medianPrMergeDays,
            i, p
        )
    }

    /**
     * Counts and median durations over [issuesOrPrs], using [windowStart] as the cutoff for the
     * per-metric predicates: "opened since windowStart", "closed since windowStart", "merged
     * since windowStart".
     *
     * Internal so it can be exercised end-to-end via [syncOne] tests.
     */
    internal fun aggregate(issuesOrPrs: List<ScmRepoIssueOrPrEntity>, windowStart: Instant): WindowAggregates {
        val issues = issuesOrPrs.filter { it.type == ScmRepoIssueOrPrType.ISSUE }
        val prs = issuesOrPrs.filter { it.type == ScmRepoIssueOrPrType.PR }

        val issueCloseDurations = issues
            .filter { (it.closedAt ?: Instant.MIN) >= windowStart }
            .mapNotNull { it.durationDays }
        val prMergeDurations = prs
            .filter { (it.mergedAt ?: Instant.MIN) >= windowStart }
            .mapNotNull { it.durationDays }

        return WindowAggregates(
            issueOpenedCount = issues.count { it.createdAt >= windowStart },
            issueClosedCount = issues.count { (it.closedAt ?: Instant.MIN) >= windowStart },
            medianIssueCloseDays = median(issueCloseDurations),
            prOpenedCount = prs.count { it.createdAt >= windowStart },
            prMergedCount = prs.count { (it.mergedAt ?: Instant.MIN) >= windowStart },
            medianPrMergeDays = median(prMergeDurations),
        )
    }

    private fun GitHubIssue.upsertInto(repo: ScmRepoIssueOrPrRepository, scmRepoId: Int) {
        val duration: Int? = closedAt?.let { Duration.between(createdAt, it).toDays().toInt().coerceAtLeast(0) }
        repo.upsert(
            scmRepoId = scmRepoId,
            ghNumber = number,
            type = ScmRepoIssueOrPrType.ISSUE,
            createdAt = createdAt,
            closedAt = closedAt,
            mergedAt = null,
            durationDays = duration,
        )
    }

    private fun GitHubPullRequest.upsertInto(repo: ScmRepoIssueOrPrRepository, scmRepoId: Int) {
        // For PRs the "closing" timestamp that drives durationDays is mergedAt when set,
        // otherwise closedAt — durationDays is consumed as the median-merge-time numerator.
        val closingTs = mergedAt ?: closedAt
        val duration: Int? = closingTs?.let { Duration.between(createdAt, it).toDays().toInt().coerceAtLeast(0) }
        repo.upsert(
            scmRepoId = scmRepoId,
            ghNumber = number,
            type = ScmRepoIssueOrPrType.PR,
            createdAt = createdAt,
            closedAt = closedAt,
            mergedAt = mergedAt,
            durationDays = duration,
        )
    }

    companion object {
        private const val WINDOW_WEEKS = 12
        private const val SYNC_CADENCE_DAYS = 7L
        private val logger = LoggerFactory.getLogger(OssHealthIssueOrPrSyncService::class.java)
    }
}

internal data class WindowAggregates(
    val issueOpenedCount: Int,
    val issueClosedCount: Int,
    val medianIssueCloseDays: Double?,
    val prOpenedCount: Int,
    val prMergedCount: Int,
    val medianPrMergeDays: Double?,
)

/**
 * Plain median: middle value for odd N, average of the two middle values for even N.
 * Returns null for an empty list. Caller filters out null durations.
 */
private fun median(values: List<Int>): Double? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val n = sorted.size
    return if (n % 2 == 1) sorted[n / 2].toDouble()
    else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
}
