package io.klibs.integration.github

import com.github.benmanes.caffeine.cache.Caffeine
import io.klibs.integration.github.health.GitHubRateLimitInfo
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import okhttp3.Request
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
) : GitHubIntegration {

    private val lastSuccessfulRequestTime = AtomicReference(Instant.now())

    // Specific request type counters
    private val repositoryRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "repository")
    private val userRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "user")
    private val licenseRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "license")
    private val readmeRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "readme")
    private val markdownRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "markdown")

    private val topicsRequestCounter = meterRegistry.counter("klibs.github.requests", "type", "topics")

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

    private fun GHRepository.markdownRender(markdownContent: String, mode: MarkdownMode): String? {
        return this.renderMarkdown(markdownContent, mode)
            .readText()
            .takeIf { it.isNotBlank() }
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

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com"
        private val logger = org.slf4j.LoggerFactory.getLogger(GitHubIntegrationKohsukeLibrary::class.java)
    }

    override fun getRepositoryTopics(repositoryId: Long): List<String> {
        topicsRequestCounter.increment()
        val topics = getRepositoryById(repositoryId)?.listTopics() ?: emptyList()
        return topics.mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
    }
}
