package io.klibs.app.configuration

import io.klibs.app.filter.GitHubWebhookSignatureFilter
import io.klibs.integration.github.service.GitHubWebhookSignatureVerifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal val GITHUB_WEBHOOK_PATH = "/webhooks/github"

@Configuration
class WebhookFilterConfiguration {

    /**
     * Registers a filter that validates GitHub webhook signatures and caches the request body.
     */
    @Bean
    fun gitHubWebhookSignatureFilter(
        signatureVerifier: GitHubWebhookSignatureVerifier
    ): FilterRegistrationBean<GitHubWebhookSignatureFilter> {
        val registrationBean = FilterRegistrationBean(GitHubWebhookSignatureFilter(signatureVerifier))
        registrationBean.addUrlPatterns("$GITHUB_WEBHOOK_PATH/*")
        registrationBean.order = 1
        return registrationBean
    }
}
