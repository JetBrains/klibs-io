package io.klibs.app.configuration

import io.klibs.core.search.opensearch.SearchIndexLock
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Instant
import javax.sql.DataSource

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@Configuration
class SchedulingConfiguration {

    /**
     * Need to declare explicitly; otherwise autoconfigured one disappears
     */
    @Bean
    fun taskScheduler(builder: ThreadPoolTaskSchedulerBuilder): ThreadPoolTaskScheduler = builder.build()

    @Bean
    fun searchIndexSyncScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("search-index-sync-")
    }

    /**
     * For locks whose name is only known at runtime, which `@SchedulerLock` cannot express: the search
     * index sync locks per index and per config hash, e.g. `searchIndexSync-project-a3f9c1e2`.
     */
    @Bean
    fun lockingTaskExecutor(lockProvider: LockProvider): LockingTaskExecutor =
        DefaultLockingTaskExecutor(lockProvider)

   @Bean
    fun searchIndexLock(lockingTaskExecutor: LockingTaskExecutor) =
        SearchIndexLock { spec, block ->
            val lock = LockConfiguration(Instant.now(), spec.name, spec.atMostFor, spec.atLeastFor)
            val task = LockingTaskExecutor.TaskWithResult<Unit> { block() }
            lockingTaskExecutor.executeWithLock(task, lock).wasExecuted()
        }

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        )
    }
}