package io.klibs.core.scm.repository.health.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "scm_repo_health_components")
class ScmRepoHealthComponentsEntity(
    @Id
    @Column(name = "scm_repo_id")
    val scmRepoId: Int,

    @Column(name = "score_recomputed_ts")
    val scoreRecomputedTs: Instant?,

    @Column(name = "issue_opened_count")
    val issueOpenedCount: Int?,

    @Column(name = "issue_closed_count")
    val issueClosedCount: Int?,

    @Column(name = "median_issue_close_days")
    val medianIssueCloseDays: Double?,

    @Column(name = "pr_opened_count")
    val prOpenedCount: Int?,

    @Column(name = "pr_merged_count")
    val prMergedCount: Int?,

    @Column(name = "median_pr_merge_days")
    val medianPrMergeDays: Double?,

    @Column(name = "active_contributors")
    val activeContributors: Int?,

    @Column(name = "top_contributor_share")
    val topContributorShare: Double?,

    /** Commit Consistency — penalises bursty/irregular commit cadence over the last 12 weeks. */
    @Column(name = "c_score")
    val cScore: Double?,

    /** Issue Responsiveness — blends 12-week issue close ratio with median time-to-close. */
    @Column(name = "i_score")
    val iScore: Double?,

    /** PR Management — blends 12-week PR merge ratio with median time-to-merge. */
    @Column(name = "p_score")
    val pScore: Double?,

    /** Author Diversity — rewards more active contributors and a smaller top-committer share (bus factor). */
    @Column(name = "a_score")
    val aScore: Double?,

    /** Composite OSS health : `100 * (0.30·C + 0.25·I + 0.25·P + 0.20·A)`. `null` if any component is missing. */
    @Column(name = "health_score")
    val healthScore: Int?,

    @Column(name = "last_issue_or_pr_sync_ts")
    val lastIssueOrPrSyncTs: Instant?,
)
