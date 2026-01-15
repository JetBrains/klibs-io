package io.klibs.app.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File

@ConfigurationProperties("klibs.indexing-configuration.gmaven")
data class GoogleMavenCacheConfigurationProperties(
    val cacheDir: File? = null,
    val s3: S3Properties = S3Properties()
) {
    data class S3Properties(
        val bucketName: String? = null,
        val prefix: String? = "gmaven"
    )
}