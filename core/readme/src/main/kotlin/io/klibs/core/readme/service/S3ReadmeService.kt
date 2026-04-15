package io.klibs.core.readme.service

import io.klibs.core.readme.ReadmeConfigurationProperties
import io.klibs.core.storage.S3StorageService

class S3ReadmeService(
    private val readmeProperties: ReadmeConfigurationProperties,
    private val s3StorageService: S3StorageService,
) : ReadmeService {
    private val bucketName = readmeProperties.s3.bucketName ?: throw IllegalArgumentException("Bucket name is required for S3 mode")

    override fun readReadmeMd(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, "md")
    }

    override fun readReadmeHtml(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, "html")
    }

    private fun readReadmeWithFallback(projectId: Int?, scmRepositoryId: Int?, format: String): String? {
        projectId?.let { id ->
            readProjectReadme(id, format)?.let { return it }
        }
        return scmRepositoryId?.let { readRepoReadme(it, format) }
    }

    private fun readProjectReadme(projectId: Int, format: String): String? =
        readReadme(getProjectS3Key(projectId, format))

    private fun readRepoReadme(scmRepositoryId: Int, format: String): String? =
        readReadme(getRepoS3Key(scmRepositoryId, format))

    private fun readReadme(key: String): String? = s3StorageService.readText(bucketName, key)

    override fun writeReadmeFiles(projectId: Int, mdContent: String, htmlContent: String) {
        writeReadmeContent(projectId = projectId, format = "md", content = mdContent)
        writeReadmeContent(projectId = projectId, format = "html", content = htmlContent)
    }

    private fun writeReadmeContent(projectId: Int, format: String, content: String) {
        val key = getProjectS3Key(projectId, format)
        s3StorageService.writeText(bucketName, key, content)
    }

    private fun getProjectS3Key(projectId: Int, format: String): String {
        validateFormat(format)
        val fileName = "readme-$projectId.$format"
        return "${readmeProperties.s3.prefix}/project/$fileName"
    }

    private fun getRepoS3Key(scmRepositoryId: Int, format: String): String {
        validateFormat(format)
        val fileName = "readme-$scmRepositoryId.$format"
        return "${readmeProperties.s3.prefix}/$fileName"
    }

    private fun validateFormat(format: String) {
        require(format == "md" || format == "html") {
            "Format can only be \"md\" or \"html\""
        }
    }
}