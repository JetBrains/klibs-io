package io.klibs.app.configuration

import io.klibs.app.util.BackoffProvider
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
class BackoffConfig {

    @Bean
    @Qualifier("aiDescriptionBackoffProvider")
    fun aiDescriptionBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("AiProjectDescription", meterRegistry)

    @Bean
    @Qualifier("aiTagsBackoffProvider")
    fun aiTagsBackoffProvider(meterRegistry: MeterRegistry): BackoffProvider =
        BackoffProvider("AiProjectTags", meterRegistry)
}
