package io.klibs.core.scm.repository.health.entity

import io.klibs.core.scm.repository.health.enums.ScmRepoIssueOrPrType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "scm_repo_issue_or_pr")
class ScmRepoIssueOrPrEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scm_repo_issue_or_pr_id_seq")
    @SequenceGenerator(
        name = "scm_repo_issue_or_pr_id_seq",
        sequenceName = "scm_repo_issue_or_pr_id_seq",
        allocationSize = 50,
    )
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "scm_repo_id")
    val scmRepoId: Int,

    /**
     * GitHub's per-repo issue/PR number (the integer in URLs like `/issues/42`).
     * Combined with [scmRepoId], this is the upsert dedup key — re-fetching the
     * same item updates in place rather than inserting a duplicate row.
     */
    @Column(name = "gh_number")
    val ghNumber: Int,

    /**
     * Distinguishes issues from pull requests. Aggregate queries filter by this
     * because the I sub-score is computed from issues and the P sub-score from PRs.
     */
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: ScmRepoIssueOrPrType,

    @Column(name = "created_at")
    val createdAt: Instant,

    @Column(name = "closed_at")
    val closedAt: Instant?,

    @Column(name = "merged_at")
    val mergedAt: Instant?,

    @Column(name = "duration_days")
    val durationDays: Int?,
)
