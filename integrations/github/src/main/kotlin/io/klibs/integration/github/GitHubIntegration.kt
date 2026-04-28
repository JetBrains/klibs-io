package io.klibs.integration.github

import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubCommitAuthor
import io.klibs.integration.github.model.GitHubIssueEvent
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubParticipationStats
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
     * Returns issues and pull requests from the given repository that were updated at or after
     * [since]. Pagination is ordered by updated-desc and stops as soon as an item older than
     * [since] is seen — only the relevant pages are fetched. Returned PR items carry
     * [GitHubIssueEvent.mergedAt] only if they were actually merged; closed-but-unmerged PRs
     * have [mergedAt]=null.
     */
    fun recentIssueEvents(repositoryId: Long, since: Instant): List<GitHubIssueEvent>

    /**
     * Returns weekly commit counts from `/stats/participation`.
     * Returns `null` if GitHub responded with 202 Accepted (stats are being computed);
     * caller should retry later.
     */
    fun getParticipationStats(repositoryId: Long): GitHubParticipationStats?

    /**
     * Returns the commit count per author on the repo's default branch since [since],
     * fetched via GraphQL commit history. Used to derive the author-diversity sub-score
     * (active contributors + top-contributor share) over the last 12 weeks.
     *
     * This is independent of the `/stats/contributors` REST endpoint, which has a known
     * GitHub-side regression as of April 2026 (see community discussion #192970) where it
     * returns 202 indefinitely without ever computing.
     *
     * Returns `null` on auth/network/rate-limit errors so the caller can retry later.
     */
    fun getCommitAuthorCounts(owner: String, name: String, since: Instant): List<GitHubCommitAuthor>?
}

