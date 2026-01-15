package io.klibs.core.storage

import io.awspring.cloud.s3.S3Template
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class S3StorageService(
    private val s3Template: S3Template
) {
    private val logger = LoggerFactory.getLogger(S3StorageService::class.java)

    fun readText(bucketName: String, key: String): String? {
        return try {
            val resource = s3Template.download(bucketName, key)
            if (!resource.exists()) {
                return null
            }
            resource.inputStream.bufferedReader().use { reader ->
                reader.readText().takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            logger.error("Failed to read from S3. Bucket: $bucketName, key: $key", e)
            null
        }
    }

    fun writeText(bucketName: String, key: String, content: String) {
        try {
            content.byteInputStream().use {
                s3Template.upload(bucketName, key, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to write to S3. Bucket: $bucketName, key: $key", e)
        }
    }
}
