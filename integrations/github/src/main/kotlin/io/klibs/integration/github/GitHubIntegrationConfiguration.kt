package io.klibs.integration.github

import okhttp3.Cache
import okhttp3.OkHttpClient
import org.kohsuke.github.GHRateLimit
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.RateLimitChecker
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [GitHubIntegrationProperties::class])
@ComponentScan(basePackages = ["io.klibs.integration.github"])
class GitHubIntegrationConfiguration {

    @Bean
    fun okHttpClient(gitHubIntegrationProperties: GitHubIntegrationProperties): OkHttpClient {
        val requestCache = createRequestCache(gitHubIntegrationProperties)
        return OkHttpClient.Builder().cache(requestCache).build()
    }

    @Bean
    fun githubApi(okHttpClient: OkHttpClient, gitHubIntegrationProperties: GitHubIntegrationProperties): GitHub {
        return GitHubBuilder()
            .also {
                if (gitHubIntegrationProperties.personalAccessToken != null) {
                    it.withOAuthToken(gitHubIntegrationProperties.personalAccessToken)
                }
            }
            .withConnector(OkHttpGitHubConnector(okHttpClient))
            // Proactively throw once CORE usage crosses RATE_LIMIT_FAIL_AT_USED — before
            // GitHub responds with a primary-rate-limit 403 that would otherwise trigger
            // kohsuke's default WAIT handler and park the calling scheduler thread until
            // X-RateLimit-Reset (up to ~60 min). An earlier WARN is logged as we approach
            // that cliff.
            .withRateLimitChecker(
                FailingRateLimitChecker(
                    failAtUsed = RATE_LIMIT_FAIL_AT_USED,
                    warnAtUsed = RATE_LIMIT_WARN_AT_USED,
                )
            )
            .build()
    }

    private fun createRequestCache(gitHubIntegrationProperties: GitHubIntegrationProperties): Cache? {
        val requestCachePath = gitHubIntegrationProperties.cache.requestCachePath ?: return null
        val cacheSizeMb = gitHubIntegrationProperties.cache.requestCacheSizeMb ?: 10
        return Cache(
            directory = requestCachePath,
            maxSize = cacheSizeMb * 1024L * 1024L
        )
    }

    internal class FailingRateLimitChecker(
        private val failAtUsed: Int,
        private val warnAtUsed: Int,
    ) : RateLimitChecker() {
        public override fun checkRateLimit(rateLimitRecord: GHRateLimit.Record, count: Long): Boolean {
            val used = rateLimitRecord.limit - rateLimitRecord.remaining
            if (used >= failAtUsed) {
                throw GitHubRateLimitExhaustedException(
                    "GitHub CORE rate limit guard tripped: used=$used of ${rateLimitRecord.limit}, " +
                        "resetsAt=${rateLimitRecord.resetDate}; failing fast before primary 403."
                )
            }
            if (used > warnAtUsed) {
                logger.warn(
                    "GitHub CORE rate limit approaching: remaining={} of {}, resetsAt={}",
                    rateLimitRecord.remaining,
                    rateLimitRecord.limit,
                    rateLimitRecord.resetDate,
                )
            }
            return false
        }
    }

    private companion object {
        private const val RATE_LIMIT_WARN_AT_USED = 3000
        private const val RATE_LIMIT_FAIL_AT_USED = 4500
        private val logger = LoggerFactory.getLogger(GitHubIntegrationConfiguration::class.java)
    }
}

class GitHubRateLimitExhaustedException(message: String) : RuntimeException(message)
