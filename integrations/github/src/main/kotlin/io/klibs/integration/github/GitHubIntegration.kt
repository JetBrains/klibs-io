package io.klibs.integration.github

import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubLicense
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
}

