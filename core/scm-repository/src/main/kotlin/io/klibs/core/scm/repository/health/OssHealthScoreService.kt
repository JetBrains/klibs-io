package io.klibs.core.scm.repository.health

import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.health.repository.ScmRepoHealthComponentsRepository
import io.klibs.integration.github.GitHubIntegration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Computes the final OSS Health score for one repo by combining:
 *  - I/P components already written by [OssHealthIssueOrPrSyncService]
 *  - Commit consistency (C) from `/stats/participation` (`getCommitsByWeek`)
 *  - Author diversity (A) from the GraphQL commit history (`getCommitAuthorCounts`)
 *
 * If either GitHub call throws (rate limit, 202, network), this run is skipped and the next
 * attempt happens on the next repo-update tick (`isDue` stays true until the score catches up
 * to the latest issue/PR sync).
 */
@Service
class OssHealthScoreService(
    private val healthComponentsRepository: ScmRepoHealthComponentsRepository,
    private val gitHubIntegration: GitHubIntegration,
    private val ossHealthProperties: OssHealthProperties,
) {

    /**
     * Whether [repo]'s OSS health score needs a recompute right now. Cadence is driven by the
     * issue/PR sync: a score is due iff issue/PR sync has run at least once AND the score is older
     * than that last issue/PR sync (or never computed). This way failed scores retry every tick
     * until they catch up.
     */
    fun isDue(repo: ScmRepositoryEntity, now: Instant): Boolean {
        val components = healthComponentsRepository.findByScmRepoId(repo.idNotNull) ?: return false
        val lastIssueOrPrSync = components.lastIssueOrPrSyncTs ?: return false
        val lastScore = components.scoreRecomputedTs ?: return true
        return lastScore.isBefore(lastIssueOrPrSync)
    }

    @Transactional
    fun computeOne(repo: ScmRepositoryEntity, now: Instant) {
        val twelveWeeksAgo = now.minus(WINDOW_DAYS, ChronoUnit.DAYS)

        val commitsByWeek = runCatching { gitHubIntegration.getCommitsByWeek(repo.nativeId) }
            .onFailure { logger.warn("commitsByWeek failed for ${repo.ownerLogin}/${repo.name}: ${it.message}") }
            .getOrNull()
        if (commitsByWeek == null) {
            logger.info(
                "Deferring OSS health score for repoId={} {}/{}: commitsByWeek null — retry on next repo update",
                repo.idNotNull, repo.ownerLogin, repo.name
            )
            return
        }

        val authorCounts = runCatching {
            gitHubIntegration.getCommitAuthorCounts(repo.ownerLogin, repo.name, twelveWeeksAgo)
        }.onFailure {
            logger.warn("commit author counts failed for ${repo.ownerLogin}/${repo.name}: ${it.message}")
        }.getOrNull()
        if (authorCounts == null) {
            logger.info(
                "Deferring OSS health score for repoId={} {}/{}: authorCounts null — retry on next repo update",
                repo.idNotNull, repo.ownerLogin, repo.name
            )
            return
        }

        val last12WeekCommits = commitsByWeek.takeLast(12)
        val c = OssHealthFormula.commitConsistency(
            weeklyCommits = last12WeekCommits,
            cvDenominator = ossHealthProperties.commitCvDenominator,
        )

        val activeContributors = authorCounts.values.count { it > 0 }
        val totalCommits12w = authorCounts.values.sum()
        val topShare: Double? = if (totalCommits12w > 0) {
            authorCounts.values.max().toDouble() / totalCommits12w
        } else null
        val a = OssHealthFormula.authorDiversity(
            activeContributors = activeContributors,
            topContributorShare = topShare,
            activeContributorsTarget = ossHealthProperties.activeContributorsTarget,
        )

        // Read current I/P (written by the issue/PR sync service on its last run).
        val existing = healthComponentsRepository.findByScmRepoId(repo.idNotNull)
        val i = existing?.iScore
        val p = existing?.pScore

        val composed = OssHealthFormula.composeScore(c, i, p, a)

        healthComponentsRepository.upsertScoreComponents(
            scmRepoId = repo.idNotNull,
            scoreRecomputedTs = now,
            activeContributors = activeContributors,
            topContributorShare = topShare,
            cScore = c,
            aScore = a,
            healthScore = composed,
        )

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
