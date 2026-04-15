package io.klibs.core.readme

import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File

@ConfigurationProperties("klibs.readme")
data class ReadmeConfigurationProperties(
    val cacheDir: File? = null,
    val s3: S3Properties
) {
    data class S3Properties(
        val bucketName: String? = null,
        val prefix: String? = ""
    )
}
