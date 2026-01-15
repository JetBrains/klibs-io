package io.klibs.app.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "klibs.indexing-configuration.executor")
class ExecutorServiceConfiguration {
    var threadCount: Int = 1

    @Bean
    fun scheduledExecutorService(): ScheduledExecutorService {
        return if (threadCount <= 1) {
            Executors.newSingleThreadScheduledExecutor()
        } else {
            Executors.newScheduledThreadPool(threadCount)
        }
    }
}