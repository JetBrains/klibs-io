package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Guardrails for AI package-description regeneration (KTL-4673).
 *
 * @param regenTtl skip regenerating a description if the previous one was generated within this window.
 */
@ConfigurationProperties("klibs.indexing-configuration.description")
data class PackageDescriptionProperties(
    val regenTtl: Duration = Duration.ofDays(90)
)
