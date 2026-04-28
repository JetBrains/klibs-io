package io.klibs.integration.github

import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubPullRequest
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import java.time.Instant

interface GitHubIntegration {

    fun getRepository(nativeId: Long): GitHubRepository?

    fun getRepository(owner: String, name: String): GitHubRepository?

    fun getUser(login: String): GitHubUser?

    fun getLicense(repositoryId: Long): GitHubLicense?

    /**
     * Fetches README in raw Markdown if it has changed since [modifiedSince]:
     * - Content: README exists and was modified (200)
     * - NotModified: README exists but was not modified since the provided timestamp (304)
     * - NotFound: README does not exist for the repository (404)
     * - Error: unexpected HTTP status or error while calling GitHub API
     */
    fun getReadmeWithModifiedSinceCheck(
        repositoryId: Long,
        modifiedSince: Instant = Instant.EPOCH
    ): ReadmeFetchResult

    fun markdownRender(markdownText: String, contextRepositoryId: Long): String?

    fun markdownToHtml(markdownText: String, contextRepositoryId: Long?): String?

    fun getRepositoryTopics(repositoryId: Long): List<String>

    fun getRateLimitInfo(): GitHubRateLimitInfo

    fun getLastSuccessfulRequestTime(): Instant

    /**
     * Returns issues (excluding pull requests) from the given repository that were updated at or
     * after [since]. Backed by `/repos/{owner}/{repo}/issues?since=…&sort=updated&direction=desc`,
     * filtered to non-PR items. The endpoint supports server-side `since`, so pagination stops
     * naturally at the window edge.
     */
    fun recentIssues(repositoryId: Long, since: Instant): List<GitHubIssue>

    /**
     * Returns pull requests from the given repository that were updated at or after [since].
     * Backed by `/repos/{owner}/{repo}/pulls?sort=updated&direction=desc`, which has no
     * server-side `since` parameter — pagination is ordered updated-desc and stops as soon as
     * an item older than [since] is seen. Returned items carry [GitHubPullRequest.mergedAt]
     * only if the PR was actually merged; closed-but-unmerged PRs have `mergedAt=null`.
     */
    fun recentPrs(repositoryId: Long, since: Instant): List<GitHubPullRequest>

    /**
     * Weekly commit counts from `/stats/participation`. Always 52 entries — a full year — oldest
     * first, so index 51 is the most recent full week. Used to derive the commit-consistency
     * sub-score (CV over the last 12 weeks = `takeLast(12)`).
     *
     * Throws on 202 Accepted (stats are still being computed), missing repo, or network errors
     * so the caller can defer and retry later.
     */
    fun getCommitsByWeek(repositoryId: Long): List<Int>

    /**
     * Returns the commit count per author (GitHub login, falling back to commit email when the
     * author isn't linked to a GitHub account) on the repo's default branch since [since], fetched
     * via GraphQL commit history. Used to derive the author-diversity sub-score (active contributors
     * + top-contributor share) over the last 12 weeks.
     *
     * This is independent of the `/stats/contributors` REST endpoint, which has a known
     * GitHub-side regression as of April 2026 (see community discussion #192970) where it
     * returns 202 indefinitely without ever computing.
     *
     * Throws on auth/network/rate-limit/GraphQL errors so the caller can defer and retry later.
     * Returns an empty map when the repo has no default branch.
     */
    fun getCommitAuthorCounts(owner: String, name: String, since: Instant): Map<String, Int>
}

