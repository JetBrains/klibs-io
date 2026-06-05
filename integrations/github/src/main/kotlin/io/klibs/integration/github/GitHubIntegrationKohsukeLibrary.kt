package io.klibs.integration.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubIssue
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubPullRequest
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.GqlCommitAuthorsResponse
import io.klibs.integration.github.model.ReadmeFetchResult
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequestQueryBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.MarkdownMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.FileNotFoundException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.java

@Component
internal class GitHubIntegrationKohsukeLibrary(
    @Autowired
    private val meterRegistry: MeterRegistry,
    @Autowired
    private val githubApi: GitHub,
    @Autowired
    private val okHttpClient: okhttp3.OkHttpClient,
    @Autowired
    private val gitHubIntegrationProperties: GitHubIntegrationProperties,
    @Autowired
    private val jsonMapper: ObjectMapper,
) : GitHubIntegration {

    private val lastSuccessfulRequestTime = AtomicReference(Instant.now())

    init {
        Gauge.builder("klibs.github.lastSuccessfulRequestTime") {
            (Instant.now().toEpochMilli() - lastSuccessfulRequestTime.get().toEpochMilli()).toDouble()
        }
            .description("Time since the last successful GitHub API request (ms)")
            .register(meterRegistry)
    }

    private val repositoryCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<Long, GHRepository?>()


    override fun getRepository(nativeId: Long): GitHubRepository? {
        val repo = getRepositoryById(nativeId)
        return repo?.toModel()
    }

    override fun getRepository(owner: String, name: String): GitHubRepository? {
        val ghRepository = executeNullable {
            githubApi.getRepository("$owner/$name")
        } ?: return null

        repositoryCache.put(ghRepository.id, ghRepository)

        return ghRepository.toModel()
    }

    override fun getUser(login: String): GitHubUser? {
        githubApi.refreshCache()

        val ghUser = executeNullable {
            githubApi.getUser(login)
        } ?: return null

        return GitHubUser(
            id = ghUser.id,
            login = ghUser.login,
            type = ghUser.type,
            name = ghUser.name?.takeIf { it.isNotBlank() } ?: ghUser.login,
            company = ghUser.company?.takeIf { it.isNotBlank() },
            blog = ghUser.blog?.takeIf { it.isNotBlank() },
            location = ghUser.location?.takeIf { it.isNotBlank() },
            email = ghUser.email?.takeIf { it.isNotBlank() },
            bio = ghUser.bio?.takeIf { it.isNotBlank() },
            twitterUsername = ghUser.twitterUsername?.takeIf { it.isNotBlank() },
            followers = ghUser.followersCount
        )
    }

    private fun GHRepository.toModel(): GitHubRepository {
        return GitHubRepository(
            nativeId = this.id,
            name = this.name,
            createdAt = this.createdAt.toInstant(),
            description = this.description?.takeIf { it.isNotBlank() },
            defaultBranch = requireNotNull(this.defaultBranch) {
                "The default branch is null for ${this.id}"
            },
            owner = this.owner.login,
            homepage = this.homepage?.takeIf { it.isNotBlank() },
            hasGhPages = this.hasPages(),
            hasIssues = this.hasIssues(),
            hasWiki = this.hasWiki(),
            stars = this.stargazersCount,
            openIssues = this.openIssueCount,
            lastActivity = this.pushedAt.toInstant(),
        )
    }

    override fun getLicense(repositoryId: Long): GitHubLicense? {
        val license = getRepositoryById(repositoryId)?.license ?: return null
        return GitHubLicense(
            key = license.key,
            name = license.name
        )
    }

    override fun getReadmeWithModifiedSinceCheck(
        repositoryId: Long,
        modifiedSince: Instant
    ): ReadmeFetchResult {
        val sample = Timer.start(meterRegistry)
        try {
            val url = "$GITHUB_API_URL/repositories/$repositoryId/readme"

            val ifModifiedSince = ZonedDateTime.ofInstant(modifiedSince, ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME)

            val requestBuilder = Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/vnd.github.raw")
                .addHeader("If-Modified-Since", ifModifiedSince)

            gitHubIntegrationProperties.personalAccessToken?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                return when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        ReadmeFetchResult.Content(body)
                    }
                    304 -> {
                        logger.debug("README of {} content not modified since {}.", repositoryId, modifiedSince)
                        ReadmeFetchResult.NotModified
                    }
                    404 -> {
                        logger.debug("README of {} not found.", repositoryId)
                        ReadmeFetchResult.NotFound
                    }
                    else -> {
                        logger.error("ERROR: ${response.code} from GitHub API at $url.")
                        ReadmeFetchResult.Error(status = response.code)
                    }
                }
            }
        } finally {
            sample.stop(meterRegistry.timer("klibs.github.request.time"))
            lastSuccessfulRequestTime.set(Instant.now())
        }
    }

    override fun markdownRender(markdownText: String, contextRepositoryId: Long): String? {
        return getRepositoryById(contextRepositoryId)?.markdownRender(markdownText, MarkdownMode.MARKDOWN)
    }

    override fun markdownToHtml(markdownText: String, contextRepositoryId: Long?): String? {
        return if (contextRepositoryId == null) {
            githubApi.renderMarkdown(markdownText).readText()
        } else {
            getRepositoryById(contextRepositoryId)?.markdownRender(markdownText, MarkdownMode.GFM)
        }
    }

    private fun getRepositoryById(id: Long): GHRepository? {
        return repositoryCache.get(id) {
            executeNullable {
                githubApi.getRepositoryById(it)
            }
        }
    }

    private fun GHRepository.markdownRender(markdownContent: String, mode: MarkdownMode): String {
        return this.renderMarkdown(markdownContent, mode).readText()
    }

    private fun <T> executeNullable(block: () -> T): T? {
        // Start timing the request
        val sample = Timer.start(meterRegistry)
        
        return try {
            block()
        } catch (e: FileNotFoundException) {
            null
        } finally {
            // Record the request time
            sample.stop(meterRegistry.timer("klibs.github.request.time"))
            lastSuccessfulRequestTime.set(Instant.now())
        }
    }

    override fun getRateLimitInfo(): GitHubRateLimitInfo {
        val rateLimit = githubApi.rateLimit
        return GitHubRateLimitInfo(
            limit = rateLimit.getLimit(),
            remaining = rateLimit.getRemaining(),
            resetAt = rateLimit.resetDate.toInstant()
        )
    }

    override fun getLastSuccessfulRequestTime(): Instant {
        return lastSuccessfulRequestTime.get()
    }

    override fun getRepositoryTopics(repositoryId: Long): List<String> {
        val topics = getRepositoryById(repositoryId)?.listTopics() ?: emptyList()
        return topics.mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
    }

    override fun recentIssues(repositoryId: Long, since: Instant): List<GitHubIssue> {
        val repo = getRepositoryById(repositoryId) ?: return emptyList()

        // GitHub treats PRs as a subtype of issues: the /issues endpoint returns BOTH issues and PRs
        // in one response, with PRs flagged via isPullRequest. We drop PR rows here — PRs come
        // through recentPrs() so we can pick up merged_at, which /issues doesn't expose cleanly.
        return buildList {
            val iterator = repo.queryIssues()
                .state(GHIssueState.ALL)
                .sort(org.kohsuke.github.GHIssueQueryBuilder.Sort.UPDATED)
                .direction(GHDirection.DESC)
                .since(Date.from(since))
                .list()
                .iterator()

            while (iterator.hasNext()) {
                val issue = try {
                    iterator.next()
                } catch (e: Exception) {
                    logger.warn("Failed to page issues for repo $repositoryId: ${e.message}")
                    break
                }
                if (issue.isPullRequest) continue
                val updatedAt = issue.updatedAt?.toInstant() ?: continue
                add(
                    GitHubIssue(
                        number = issue.number,
                        createdAt = issue.createdAt.toInstant(),
                        closedAt = issue.closedAt?.toInstant(),
                        updatedAt = updatedAt,
                    )
                )
            }
        }
    }

    override fun recentPrs(repositoryId: Long, since: Instant): List<GitHubPullRequest> {
        val repo = getRepositoryById(repositoryId) ?: return emptyList()

        // No `.since()` support on /pulls — sort updated-desc and stop as soon as we cross the window.
        return buildList {
            val iterator = repo.queryPullRequests()
                .state(GHIssueState.ALL)
                .sort(GHPullRequestQueryBuilder.Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .iterator()
            while (iterator.hasNext()) {
                val pr = try {
                    iterator.next()
                } catch (e: Exception) {
                    logger.warn("Failed to page PRs for repo $repositoryId: ${e.message}")
                    break
                }
                val updatedAt = pr.updatedAt?.toInstant() ?: continue
                if (updatedAt.isBefore(since)) break
                add(
                    GitHubPullRequest(
                        number = pr.number,
                        createdAt = pr.createdAt.toInstant(),
                        closedAt = pr.closedAt?.toInstant(),
                        mergedAt = pr.mergedAt?.toInstant(),
                        updatedAt = updatedAt,
                    )
                )
            }
        }
    }

    override fun getCommitsByWeek(repositoryId: Long): List<Int> {
        val repo = getRepositoryById(repositoryId)
            ?: error("Repository not found for repoId=$repositoryId")
        return repo.statistics.participation.allCommits
            ?: error("Participation stats unavailable for repoId=$repositoryId")
    }

    override fun getCommitAuthorCounts(owner: String, name: String, since: Instant): Map<String, Int> {
        val sinceIso = DateTimeFormatter.ISO_INSTANT.format(since)
        val counts = mutableMapOf<String, Int>()
        var cursor: String? = null

        while (true) {
            val variables = buildMap<String, Any> {
                put("owner", owner)
                put("name", name)
                put("since", sinceIso)
                cursor?.let { put("cursor", it) }
            }
            val responseBody = postGraphQl(COMMIT_AUTHORS_QUERY, variables)
                ?: error("GitHub GraphQL request failed for $owner/$name")
            val response = jsonMapper.readValue(responseBody, GqlCommitAuthorsResponse::class.java)
            if (!response.errors.isNullOrEmpty()) {
                error("GraphQL errors for $owner/$name: ${response.errors.toString().take(300)}")
            }
            val history = response.data?.repository?.defaultBranchRef?.target?.history
                ?: return emptyMap()

            history.nodes.forEach { node ->
                val identity = node.author?.user?.login ?: node.author?.email ?: return@forEach
                counts[identity] = (counts[identity] ?: 0) + 1
            }
            if (!history.pageInfo.hasNextPage) break
            cursor = history.pageInfo.endCursor ?: break
        }
        return counts
    }

    /** POSTs a GraphQL query+variables to GitHub. Returns the raw response body or null on non-200. */
    private fun postGraphQl(query: String, variables: Map<String, Any>): String? {
        val payload = jsonMapper.writeValueAsString(mapOf("query" to query, "variables" to variables))
        val sample = Timer.start(meterRegistry)
        try {
            val builder = Request.Builder()
                .url("$GITHUB_API_URL/graphql")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .addHeader("Accept", "application/vnd.github+json")
            gitHubIntegrationProperties.personalAccessToken?.takeIf { it.isNotBlank() }?.let { token ->
                builder.addHeader("Authorization", "Bearer $token")
            }
            okHttpClient.newCall(builder.build()).execute().use { response ->
                return when (response.code) {
                    200 -> response.body?.string()
                    else -> {
                        logger.warn("GraphQL POST returned {}", response.code)
                        null
                    }
                }
            }
        } finally {
            sample.stop(meterRegistry.timer("klibs.github.request.time"))
            lastSuccessfulRequestTime.set(Instant.now())
        }
    }

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com"
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubIntegrationKohsukeLibrary::class.java)

        private val COMMIT_AUTHORS_QUERY = """
            query CommitAuthors(${'$'}owner: String!, ${'$'}name: String!, ${'$'}since: GitTimestamp!, ${'$'}cursor: String) {
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                defaultBranchRef {
                  target {
                    ... on Commit {
                      history(since: ${'$'}since, first: 100, after: ${'$'}cursor) {
                        nodes {
                          author {
                            user { login }
                            email
                          }
                        }
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
