package io.klibs.app.configuration

import io.klibs.app.configuration.properties.IndexingConfigurationProperties
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
class ExecutorServiceConfiguration() {

    @Bean
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Bean
    fun scheduledExecutorService(indexingConfigurationProperties: IndexingConfigurationProperties): ScheduledExecutorService {
        val threadCount = indexingConfigurationProperties.executor.threadCount
        return if (threadCount <= 1) {
            Executors.newSingleThreadScheduledExecutor()
        } else {
            Executors.newScheduledThreadPool(threadCount)
        }
    }
}