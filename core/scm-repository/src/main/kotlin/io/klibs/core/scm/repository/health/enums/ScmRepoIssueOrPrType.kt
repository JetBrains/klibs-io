package io.klibs.core.scm.repository.health.enums

/**
 * Distinguishes issues from pull requests in `scm_repo_issue_or_pr`. Aggregate queries filter
 * by this because the I sub-score is computed from issues and the P sub-score from PRs.
 */
enum class ScmRepoIssueOrPrType {
    ISSUE, PR;
}
