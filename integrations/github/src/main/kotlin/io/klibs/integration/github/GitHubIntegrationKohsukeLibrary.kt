package io.klibs.integration.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubCommitAuthor
import io.klibs.integration.github.model.GitHubIssueEvent
import io.klibs.integration.github.model.GitHubIssueEventType
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubParticipationStats
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
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

    // Specific request type counters
    private val repositoryRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "repository")
    private val userRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "user")
    private val licenseRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "license")
    private val readmeRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "readme")
    private val markdownRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "markdown")

    private val topicsRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "topics")
    private val issueEventsRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "issue-events")
    private val participationRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "stats-participation")
    private val contributorsRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "graphql-commit-authors")

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
        .build<Long, GHRepository>()


    override fun getRepository(nativeId: Long): GitHubRepository? {
        repositoryRequestCounter.increment()
        
        val repo = getRepositoryById(nativeId)
        return repo?.toModel()
    }

    override fun getRepository(owner: String, name: String): GitHubRepository? {
        repositoryRequestCounter.increment()
        
        val ghRepository = executeNullable {
            githubApi.getRepository("$owner/$name")
        } ?: return null

        repositoryCache.put(ghRepository.id, ghRepository)

        return ghRepository.toModel()
    }

    override fun getUser(login: String): GitHubUser? {
        userRequestCounter.increment()
        
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
        licenseRequestCounter.increment()
        
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
        readmeRequestCounter.increment()

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
        markdownRequestCounter.increment()
        
        return getRepositoryById(contextRepositoryId)?.markdownRender(markdownText, MarkdownMode.MARKDOWN)
    }

    override fun markdownToHtml(markdownText: String, contextRepositoryId: Long?): String? {
        markdownRequestCounter.increment()
        
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
        topicsRequestCounter.increment()
        val topics = getRepositoryById(repositoryId)?.listTopics() ?: emptyList()
        return topics.mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
    }

    override fun recentIssueEvents(repositoryId: Long, since: Instant): List<GitHubIssueEvent> {
        issueEventsRequestCounter.increment()
        val repo = getRepositoryById(repositoryId) ?: return emptyList()

        // GitHub treats PRs as a subtype of issues: the /issues endpoint returns BOTH issues and PRs
        // in one response, with PRs flagged via isPullRequest. We filter PRs out here and process them
        // separately via queryPullRequests() below to get the merged_at timestamp (the issues
        // endpoint doesn't expose it cleanly).
        return buildList {
            val issueIterator = repo.queryIssues()
                .state(GHIssueState.ALL)
                .sort(org.kohsuke.github.GHIssueQueryBuilder.Sort.UPDATED)
                .direction(GHDirection.DESC)
                .since(Date.from(since))
                .list()
                .iterator()

            while (issueIterator.hasNext()) {
                val issue = try {
                    issueIterator.next()
                } catch (e: Exception) {
                    logger.warn("Failed to page issues for repo $repositoryId: ${e.message}")
                    break
                }
                if (issue.isPullRequest) continue
                val updatedAt = issue.updatedAt?.toInstant() ?: continue
                add(
                    GitHubIssueEvent(
                        type = GitHubIssueEventType.ISSUE,
                        number = issue.number,
                        createdAt = issue.createdAt.toInstant(),
                        closedAt = issue.closedAt?.toInstant(),
                        mergedAt = null,
                        updatedAt = updatedAt,
                    )
                )
            }

            val prIter = repo.queryPullRequests()
                .state(GHIssueState.ALL)
                .sort(GHPullRequestQueryBuilder.Sort.UPDATED)
                .direction(GHDirection.DESC)
                .list()
                .iterator()
            while (prIter.hasNext()) {
                val pr = try {
                    prIter.next()
                } catch (e: Exception) {
                    logger.warn("Failed to page PRs for repo $repositoryId: ${e.message}")
                    break
                }
                val updatedAt = pr.updatedAt?.toInstant() ?: continue
                // No `.since()` support on queryPullRequests() — stop early once we cross the window.
                if (updatedAt.isBefore(since)) break
                add(
                    GitHubIssueEvent(
                        type = GitHubIssueEventType.PR,
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

    override fun getParticipationStats(repositoryId: Long): GitHubParticipationStats? {
        participationRequestCounter.increment()
        val repo = getRepositoryById(repositoryId) ?: return null
        return runCatching {
            val weekly = repo.statistics.participation.allCommits ?: emptyList()
            GitHubParticipationStats(weeklyAllCommits = weekly)
        }.onFailure {
            logger.info("Participation stats unavailable for repoId={}: {}", repositoryId, it.message)
        }.getOrNull()
    }

    override fun getCommitAuthorCounts(owner: String, name: String, since: Instant): List<GitHubCommitAuthor>? {
        contributorsRequestCounter.increment()
        val sinceIso = DateTimeFormatter.ISO_INSTANT.format(since)
        val counts = mutableMapOf<String, Int>()
        var cursor: String? = null

        return runCatching {
            while (true) {
                val variables = buildMap<String, Any> {
                    put("owner", owner)
                    put("name", name)
                    put("since", sinceIso)
                    cursor?.let { put("cursor", it) }
                }
                val responseBody = postGraphQl(COMMIT_AUTHORS_QUERY, variables) ?: return@runCatching null
                val response = jsonMapper.readValue(responseBody, GqlCommitAuthorsResponse::class.java)
                if (!response.errors.isNullOrEmpty()) {
                    logger.warn("GraphQL errors for $owner/$name: {}", response.errors.toString().take(300))
                    return@runCatching null
                }
                val history = response.data?.repository?.defaultBranchRef?.target?.history
                    ?: return@runCatching emptyList<GitHubCommitAuthor>()

                history.nodes.forEach { node ->
                    val identity = node.author?.user?.login ?: node.author?.email ?: return@forEach
                    counts[identity] = (counts[identity] ?: 0) + 1
                }
                if (!history.pageInfo.hasNextPage) break
                cursor = history.pageInfo.endCursor ?: break
            }
            counts.map { (k, v) -> GitHubCommitAuthor(k, v) }.sortedByDescending { it.commits }
        }.onFailure {
            logger.info("Commit author counts unavailable for {}/{}: {}", owner, name, it.message)
        }.getOrNull()
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

// --- GraphQL response shape, scoped to this file. Maps directly to COMMIT_AUTHORS_QUERY. ---

private data class GqlCommitAuthorsResponse(
    val data: GqlCommitsData? = null,
    val errors: List<com.fasterxml.jackson.databind.JsonNode>? = null,
)

private data class GqlCommitsData(val repository: GqlRepository?)
private data class GqlRepository(val defaultBranchRef: GqlBranchRef?)
private data class GqlBranchRef(val target: GqlTarget?)
private data class GqlTarget(val history: GqlHistory?)
private data class GqlHistory(
    val nodes: List<GqlCommitNode> = emptyList(),
    val pageInfo: GqlPageInfo,
)
private data class GqlCommitNode(val author: GqlAuthor?)
private data class GqlAuthor(val user: GqlUser?, val email: String?)
private data class GqlUser(val login: String?)
private data class GqlPageInfo(val hasNextPage: Boolean, val endCursor: String?)
