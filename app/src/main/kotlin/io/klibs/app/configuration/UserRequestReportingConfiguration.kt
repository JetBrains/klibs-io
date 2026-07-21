package io.klibs.app.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Tuning for the reporting queue: how long to defer a report whose package is not yet
 * visible in the search index, so the job retries it later instead of busy-looping.
 */
@Configuration
@ConfigurationProperties(prefix = "klibs.indexing-configuration.reporting")
class UserRequestReportingConfiguration {
    var indexDefer: Duration = Duration.ofMinutes(5)
}
