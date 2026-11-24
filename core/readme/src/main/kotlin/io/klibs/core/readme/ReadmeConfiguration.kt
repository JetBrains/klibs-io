package io.klibs.core.readme

import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ReadmeConfigurationProperties::class)
class ReadmeConfiguration {

    /**
     * Separate client from GitHub [OkHttpClient], so androidx requests don't go through
     * GitHub request metering and response cache.
     */
    @Bean
    fun androidxDocsHttpClient(): OkHttpClient = OkHttpClient()
}
