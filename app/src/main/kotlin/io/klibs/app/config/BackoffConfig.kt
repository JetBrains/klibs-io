package io.klibs.app.config

import io.klibs.app.util.BackoffProvider
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
class BackoffConfig {

    @Bean
    @Qualifier("ownerBackoffProvider")
    fun ownerBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("SyncGitHubOwner", meterRegistry)

    @Bean
    @Qualifier("repoBackoffProvider")
    fun repoBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("SyncGitHubRepository", meterRegistry)

    @Bean
    @Qualifier("aiDescriptionBackoffProvider")
    fun aiDescriptionBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("AiProjectDescription", meterRegistry)

    @Bean
    @Qualifier("aiTagsBackoffProvider")
    fun aiTagsBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("AiProjectTags", meterRegistry)
}
