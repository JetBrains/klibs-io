package io.klibs.app.configuration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Bean
    fun scheduledExecutorService(): ScheduledExecutorService {
        return if (threadCount <= 1) {
            Executors.newSingleThreadScheduledExecutor()
        } else {
            Executors.newScheduledThreadPool(threadCount)
        }
    }
}