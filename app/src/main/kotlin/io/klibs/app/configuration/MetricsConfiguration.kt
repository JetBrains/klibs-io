package io.klibs.app.configuration

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.config.MeterFilterReply
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {
    companion object {
        private val enabledMetricsPrefixes = setOf(
            "tasks.scheduled",
            "hikaricp.connections", // db connections
            "hikaricp.connections.acquire",
            "hikaricp.connections.creation",
            "hikaricp.connections.idle",
            "hikaricp.connections.max",
            "hikaricp.connections.min",
            "hikaricp.connections.pending",
            "hikaricp.connections.timeout",
            "hikaricp.connections.usage",
            "system.cpu.usage",
            "http.server.requests.active", // http requests
            "jvm.memory.used", // heap and meta usage
            "jvm.gc.pause", // gc pauses duration
            "jvm.gc.live.data.size",
            "jvm.gc.max.data.size",
            "jvm.gc.memory.allocated",
            "jvm.gc.memory.promoted",
            "jvm.gc.overhead",
            "jvm.memory.committed",
            "jvm.memory.max",
            "jvm.memory.usage.after.gc",
            "jvm.threads.daemon",
            "jvm.threads.live",
            "jvm.threads.peak",
            "jvm.threads.started",
            "jvm.threads.states",
        )

        private const val KLIBS_PREFIX = "klibs"
    }

    @Bean
    fun renameTaskMetricsFilter(): MeterFilter {
        return object : MeterFilter {
            override fun accept(id: Meter.Id): MeterFilterReply {
                return if (id.name.startsWith(KLIBS_PREFIX)) {
                    MeterFilterReply.ACCEPT
                } else {
                    MeterFilterReply.DENY
                }
            }

            override fun map(id: Meter.Id): Meter.Id {
                if (enabledMetricsPrefixes.any { id.name.startsWith(it) }) {
                    return id.withName(KLIBS_PREFIX + '.' + id.name)
                }
                return id
            }
        }
    }

    @Bean
    fun timedAspect(meterRegistry: MeterRegistry): TimedAspect {
        return TimedAspect(meterRegistry)
    }
}
