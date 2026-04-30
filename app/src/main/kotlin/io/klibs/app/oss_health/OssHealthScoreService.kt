package io.klibs.app.oss_health

import io.klibs.app.configuration.properties.OssHealthProperties
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.integration.github.GitHubIntegration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Computes the final OSS Health score for one repo by combining:
 *  - I/P components already written by [OssHealthEventSyncService]
 *  - Commit consistency (C) from `/stats/participation`
 *  - Author diversity (A) from the GraphQL commit history (`getCommitAuthorCounts`)
 *
 * If either GitHub call fails / returns null (rate limit, 202, network), this run is
 * skipped and `next_health_compute_ts` is bumped forward 15 minutes so the queue retries.
 */
@Service
class OssHealthScoreService(
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository,
    private val gitHubIntegration: GitHubIntegration,
    private val ossHealthProperties: OssHealthProperties,
) {

    @Transactional
    fun computeOldestRepo(): Boolean {
        val repos = scmRepositoryRepository.findMultipleForHealthScoreCompute(limit = 1)
        val repo = repos.firstOrNull() ?: return false
        computeOne(repo, Instant.now())
        return true
    }

    fun computeOne(repo: ScmRepositoryEntity, now: Instant) {
        val twelveWeeksAgo = now.minus(WINDOW_DAYS, ChronoUnit.DAYS)

        val participation = runCatching { gitHubIntegration.getParticipationStats(repo.nativeId) }
            .onFailure { logger.warn("participation stats failed for ${repo.ownerLogin}/${repo.name}: ${it.message}") }
            .getOrNull()
        if (participation == null) {
            // /stats/participation 202s on first touch while GitHub builds the cache async;
            // it's usually ready within a minute or two, so a short backoff is enough.
            val retryAt = now.plus(2, ChronoUnit.MINUTES)
            healthComponentsRepository.setNextHealthComputeTs(repo.idNotNull, retryAt)
            logger.info(
                "Deferring OSS health score for repoId={} {}/{}: participation null — retry at {}",
                repo.idNotNull, repo.ownerLogin, repo.name, retryAt
            )
            return
        }

        val authorCounts = runCatching {
            gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo)
        }.onFailure {
            logger.warn("commit author counts failed for ${repo.ownerLogin}/${repo.name}: ${it.message}")
        }.getOrNull()
        if (authorCounts == null) {
            // GraphQL failure is network/rate-limit, not async cache build — back off long enough
            // for a hourly rate-limit window to reset.
            val retryAt = now.plus(1, ChronoUnit.HOURS)
            healthComponentsRepository.setNextHealthComputeTs(repo.idNotNull, retryAt)
            logger.info(
                "Deferring OSS health score for repoId={} {}/{}: authorCounts null — retry at {}",
                repo.idNotNull, repo.ownerLogin, repo.name, retryAt
            )
            return
        }

        val last12WeekCommits = participation.weeklyAllCommits.takeLast(12)
        val c = OssHealthFormula.commitConsistency(
            weeklyCommits = last12WeekCommits,
            cvDenominator = ossHealthProperties.commitCvDenominator,
        )

        val activeContributors = authorCounts.count { it.commits > 0 }
        val totalCommits12w = authorCounts.sumOf { it.commits }
        val topShare: Double? = if (totalCommits12w > 0) {
            authorCounts.maxOf { it.commits }.toDouble() / totalCommits12w
        } else null
        val a = OssHealthFormula.authorDiversity(
            activeContributors = activeContributors,
            topContributorShare = topShare,
            activeContributorsTarget = ossHealthProperties.activeContributorsTarget,
        )

        // Read current I/P (written by the event-sync service on its last run).
        val existing = healthComponentsRepository.findById(repo.idNotNull)
        val i = existing?.iScore
        val p = existing?.pScore

        val composed = OssHealthFormula.composeScore(c, i, p, a)

        // Compute a CV snapshot for visibility, even when c is derived separately.
        val cv: Double? = run {
            if (last12WeekCommits.isEmpty()) return@run null
            val mean = last12WeekCommits.sumOf { it.toDouble() } / last12WeekCommits.size
            if (mean <= 0) null
            else {
                val variance = last12WeekCommits.sumOf { (it - mean) * (it - mean) } / last12WeekCommits.size
                kotlin.math.sqrt(variance) / mean
            }
        }

        healthComponentsRepository.upsertScoreComponents(
            scmRepoId = repo.idNotNull,
            scoreRecomputedTs = now,
            commitsCv = cv,
            activeContributors = activeContributors,
            topContributorShare = topShare,
            cScore = c,
            aScore = a,
            healthScore = composed,
        )

        // Schedule the next recompute in ~7 days.
        healthComponentsRepository.setNextHealthComputeTs(repo.idNotNull, now.plus(7, ChronoUnit.DAYS))

        logger.info(
            "OSS health score for repoId={} {}/{}: C={} I={} P={} A={} → score={}",
            repo.idNotNull, repo.ownerLogin, repo.name, c, i, p, a, composed
        )
    }

    companion object {
        private const val WINDOW_DAYS = 12L * 7
        private val logger = LoggerFactory.getLogger(OssHealthScoreService::class.java)
    }
}
