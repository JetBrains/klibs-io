package io.klibs.integration.github.configuration

import io.klibs.integration.github.GitHubRequestMeteringInterceptor
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import io.micrometer.core.instrument.MeterRegistry
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.kohsuke.github.GHRateLimit
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.GitHubRateLimitHandler
import org.kohsuke.github.RateLimitChecker
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider
import org.kohsuke.github.authorization.AuthorizationProvider
import org.kohsuke.github.authorization.ImmutableAuthorizationProvider
import org.kohsuke.github.extras.authorization.JWTTokenProvider
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(value = [GitHubIntegrationProperties::class])
@ComponentScan(basePackages = ["io.klibs.integration.github"])
class GitHubIntegrationConfiguration {

    @Bean
    fun okHttpClient(
        gitHubIntegrationProperties: GitHubIntegrationProperties,
        meterRegistry: MeterRegistry,
    ): OkHttpClient {
        val requestCache = createRequestCache(gitHubIntegrationProperties)
        return OkHttpClient.Builder()
            .cache(requestCache)
            .addInterceptor(GitHubRequestMeteringInterceptor(meterRegistry))
            .build()
    }

    @Bean
    fun gitHubAuthorizationProvider(
        gitHubIntegrationProperties: GitHubIntegrationProperties,
    ): AuthorizationProvider {
        gitHubIntegrationProperties.personalAccessToken?.takeIf { it.isNotBlank() }?.let {
            return ImmutableAuthorizationProvider.fromOauthToken(it)
        }

        val app = requireNotNull(gitHubIntegrationProperties.app) {
            "GitHub authentication requires either a personal access token or GitHub App configuration"
        }
        val clientId = requireNotNull(app.clientId?.takeIf { it.isNotBlank() }) {
            "GitHub App client ID must not be blank"
        }
        val installationId = requireNotNull(app.installationId?.takeIf { it > 0 }) {
            "GitHub App installation ID must be positive"
        }
        val privateKey = requireNotNull(app.privateKey?.takeIf { it.isNotBlank() }) {
            "GitHub App private key must not be blank"
        }

        return AppInstallationAuthorizationProvider(
            { githubApp -> githubApp.getInstallationById(installationId) },
            JWTTokenProvider(clientId, privateKey),
        )
    }

    @Bean
    @Primary
    fun githubApi(okHttpClient: OkHttpClient, gitHubAuthorizationProvider: AuthorizationProvider): GitHub {
        return GitHubBuilder()
            .withAuthorizationProvider(gitHubAuthorizationProvider)
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

    @Bean
    fun anonymousGithubApi(okHttpClient: OkHttpClient): GitHub {
        return GitHubBuilder()
            .withConnector(OkHttpGitHubConnector(okHttpClient))
            .withRateLimitChecker(RateLimitChecker.LiteralValue(0))
            .withRateLimitHandler(GitHubRateLimitHandler.WAIT)
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
        private const val RATE_LIMIT_WARN_AT_USED = 4500
        private const val RATE_LIMIT_FAIL_AT_USED = 4990
        private val logger = LoggerFactory.getLogger(GitHubIntegrationConfiguration::class.java)
    }
}

class GitHubRateLimitExhaustedException(message: String) : RuntimeException(message)
