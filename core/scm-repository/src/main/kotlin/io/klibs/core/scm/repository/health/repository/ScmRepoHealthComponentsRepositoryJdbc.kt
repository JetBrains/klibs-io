package io.klibs.core.scm.repository.health.repository

import io.klibs.core.scm.repository.health.entity.ScmRepoHealthComponentsEntity
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class ScmRepoHealthComponentsRepositoryJdbc(
    private val jdbcClient: JdbcClient,
) : ScmRepoHealthComponentsRepository {

    override fun findById(scmRepoId: Int): ScmRepoHealthComponentsEntity? {
        return jdbcClient.sql("SELECT * FROM scm_repo_health_components WHERE scm_repo_id = :scmRepoId")
            .param("scmRepoId", scmRepoId)
            .query(DataClassRowMapper(ScmRepoHealthComponentsEntity::class.java))
            .optional()
            .getOrNull()
    }

    override fun setLastEventSyncTs(scmRepoId: Int, ts: Instant) {
        val sql = """
            INSERT INTO scm_repo_health_components (scm_repo_id, last_event_sync_ts)
            VALUES (:scmRepoId, :ts)
            ON CONFLICT (scm_repo_id) DO UPDATE SET last_event_sync_ts = :ts;
        """.trimIndent()
        jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("ts", Timestamp.from(ts))
            .update()
    }

    override fun setNextHealthComputeTs(scmRepoId: Int, ts: Instant) {
        val sql = """
            INSERT INTO scm_repo_health_components (scm_repo_id, next_health_compute_ts)
            VALUES (:scmRepoId, :ts)
            ON CONFLICT (scm_repo_id) DO UPDATE SET next_health_compute_ts = :ts;
        """.trimIndent()
        jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("ts", Timestamp.from(ts))
            .update()
    }

    override fun upsertIssuePrComponents(
        scmRepoId: Int,
        issueOpenedCount: Int,
        issueClosedCount: Int,
        medianIssueCloseDays: Double?,
        prOpenedCount: Int,
        prMergedCount: Int,
        medianPrMergeDays: Double?,
        iScore: Double?,
        pScore: Double?,
    ) {
        val sql = """
            INSERT INTO scm_repo_health_components (scm_repo_id,
                                                    issue_opened_count,
                                                    issue_closed_count,
                                                    median_issue_close_days,
                                                    pr_opened_count,
                                                    pr_merged_count,
                                                    median_pr_merge_days,
                                                    i_score,
                                                    p_score)
            VALUES (:scmRepoId,
                    :issueOpenedCount,
                    :issueClosedCount,
                    :medianIssueCloseDays,
                    :prOpenedCount,
                    :prMergedCount,
                    :medianPrMergeDays,
                    :iScore,
                    :pScore)
            ON CONFLICT (scm_repo_id) DO UPDATE SET issue_opened_count      = :issueOpenedCount,
                                                    issue_closed_count      = :issueClosedCount,
                                                    median_issue_close_days = :medianIssueCloseDays,
                                                    pr_opened_count         = :prOpenedCount,
                                                    pr_merged_count         = :prMergedCount,
                                                    median_pr_merge_days    = :medianPrMergeDays,
                                                    i_score                 = :iScore,
                                                    p_score                 = :pScore;
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("issueOpenedCount", issueOpenedCount)
            .param("issueClosedCount", issueClosedCount)
            .param("medianIssueCloseDays", medianIssueCloseDays)
            .param("prOpenedCount", prOpenedCount)
            .param("prMergedCount", prMergedCount)
            .param("medianPrMergeDays", medianPrMergeDays)
            .param("iScore", iScore)
            .param("pScore", pScore)
            .update()
    }

    override fun upsertScoreComponents(
        scmRepoId: Int,
        scoreRecomputedTs: Instant,
        commitsCv: Double?,
        activeContributors: Int?,
        topContributorShare: Double?,
        cScore: Double?,
        aScore: Double?,
        healthScore: Int?,
    ) {
        val sql = """
            INSERT INTO scm_repo_health_components (scm_repo_id,
                                                    score_recomputed_ts,
                                                    commits_cv,
                                                    active_contributors,
                                                    top_contributor_share,
                                                    c_score,
                                                    a_score,
                                                    health_score)
            VALUES (:scmRepoId,
                    :scoreRecomputedTs,
                    :commitsCv,
                    :activeContributors,
                    :topContributorShare,
                    :cScore,
                    :aScore,
                    :healthScore)
            ON CONFLICT (scm_repo_id) DO UPDATE SET score_recomputed_ts   = :scoreRecomputedTs,
                                                    commits_cv            = :commitsCv,
                                                    active_contributors   = :activeContributors,
                                                    top_contributor_share = :topContributorShare,
                                                    c_score               = :cScore,
                                                    a_score               = :aScore,
                                                    health_score          = :healthScore;
        """.trimIndent()

        jdbcClient.sql(sql)
            .param("scmRepoId", scmRepoId)
            .param("scoreRecomputedTs", Timestamp.from(scoreRecomputedTs))
            .param("commitsCv", commitsCv)
            .param("activeContributors", activeContributors)
            .param("topContributorShare", topContributorShare)
            .param("cScore", cScore)
            .param("aScore", aScore)
            .param("healthScore", healthScore)
            .update()
    }

}
