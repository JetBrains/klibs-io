package io.klibs.integration.github

import okhttp3.Cache
import okhttp3.OkHttpClient
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector
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
}
