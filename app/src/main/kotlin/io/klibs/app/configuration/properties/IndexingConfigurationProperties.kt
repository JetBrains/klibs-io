package io.klibs.app.configuration.properties

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(IndexingConfigurationProperties.PREFIX)
data class IndexingConfigurationProperties(
    val executor: ExecutorProperties = ExecutorProperties(),
    val retry: RetryProperties = RetryProperties(),
    val reporting: ReportingProperties = ReportingProperties(),
    val description: DescriptionProperties = DescriptionProperties(),
    val centralSonatype: MavenCentralProperties = MavenCentralProperties(),
    val googleMavenCentralMirror: SourceProperties = SourceProperties(),
    val gmaven: GoogleMavenProperties = GoogleMavenProperties(),
) {
    data class ExecutorProperties(
        val threadCount: Int = 1,
    )

    data class RetryProperties(
        val maxAttempts: Int = 2,
    )

    data class ReportingProperties(
        val indexDefer: Duration = Duration.ofMinutes(5),
    )

    data class DescriptionProperties(
        val regenTtl: Duration = Duration.ofDays(90),
    )

    data class SourceProperties(
        val enabled: Boolean = false,
    )

    data class MavenCentralProperties(
        val enabled: Boolean = false,
        val type: MavenCentralType = MavenCentralType.ORIGIN
    ) {
        enum class MavenCentralType {
            ORIGIN,
            GOOGLE_MIRROR
        }
    }

    data class GoogleMavenProperties(
        val enabled: Boolean = false,
        val s3: S3Properties = S3Properties()
    ) {
        data class S3Properties(
            val bucketName: String? = null,
            val prefix: String? = "gmaven"
        )
    }

    companion object {
        const val PREFIX = "klibs.indexing-configuration"

        const val CENTRAL_SONATYPE_ENABLED = "$PREFIX.central-sonatype.enabled"
        const val CENTRAL_SONATYPE_TYPE= "$PREFIX.central-sonatype.type"
        const val GMAVEN_ENABLED = "$PREFIX.gmaven.enabled"
    }
}