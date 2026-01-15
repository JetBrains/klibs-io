package io.klibs.core.scm.repository.readme

import io.klibs.core.storage.S3StorageService
import org.springframework.stereotype.Service

@Service
class S3ReadmeService(
    private val readmeProperties: ReadmeConfigurationProperties,
    private val s3StorageService: S3StorageService
) : ReadmeService {
    private val bucketName = readmeProperties.s3.bucketName ?: throw IllegalArgumentException("Bucket name is required for S3 mode")

    override fun readReadmeMd(scmRepositoryId: Int): String? =
        getReadmeContent(scmRepositoryId = scmRepositoryId, format = "md")

    override fun readReadmeHtml(scmRepositoryId: Int): String? =
        getReadmeContent(scmRepositoryId = scmRepositoryId, format = "html")

    private fun getReadmeContent(scmRepositoryId: Int, format: String): String? {
        val key = getS3Key(scmRepositoryId, format)
        return s3StorageService.readText(bucketName, key)
    }

    override fun writeReadmeFiles(scmRepositoryId: Int, mdContent: String, htmlContent: String) {
        writeReadmeContent(scmRepositoryId = scmRepositoryId, format = "md", content = mdContent)
        writeReadmeContent(scmRepositoryId = scmRepositoryId, format = "html", content = htmlContent)
    }

    private fun writeReadmeContent(scmRepositoryId: Int, format: String, content: String) {
        val key = getS3Key(scmRepositoryId, format)
        s3StorageService.writeText(bucketName, key, content)
    }

    private fun getS3Key(scmRepositoryId: Int, format: String): String {
        require(format == "md" || format == "html") {
            "Format can only be \"md\" or \"html\""
        }
        val fileName = "readme-$scmRepositoryId.$format"
        return "${readmeProperties.s3.prefix}/$fileName"
    }
}
