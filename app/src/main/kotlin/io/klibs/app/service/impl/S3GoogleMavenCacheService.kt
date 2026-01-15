package io.klibs.app.service.impl

import io.klibs.app.configuration.properties.GoogleMavenCacheConfigurationProperties
import io.klibs.app.service.GoogleMavenCacheService
import io.klibs.core.storage.S3StorageService
import io.klibs.integration.maven.androidx.GoogleMavenMasterIndexMetadata
import org.springframework.stereotype.Component

@Component
class S3GoogleMavenCacheService(
    private val cacheProperties: GoogleMavenCacheConfigurationProperties,
    private val s3StorageService: S3StorageService
) : GoogleMavenCacheService {
    private val bucketName = cacheProperties.s3.bucketName
        ?: throw IllegalArgumentException("Bucket name is required for S3 mode")

    override fun readGroupIndexFromCache(groupId: String): GoogleMavenMasterIndexMetadata? {
        val key = getS3Key(groupId)
        return s3StorageService.readText(bucketName, key)?.let {
            GoogleMavenMasterIndexMetadata.fromXml(it)
        }
    }

    override fun writeGroupIndexToCache(groupId: String, content: String) {
        val key = getS3Key(groupId)
        s3StorageService.writeText(bucketName, key, content)
    }

    private fun getS3Key(groupId: String): String {
        val fileName = "group-index-${groupId.replace(".", "-")}.xml"
        return "${cacheProperties.s3.prefix}/$fileName"
    }
}