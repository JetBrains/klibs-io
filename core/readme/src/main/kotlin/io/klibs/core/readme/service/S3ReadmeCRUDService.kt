package io.klibs.core.readme.service

import io.klibs.core.readme.ReadmeConfigurationProperties
import io.klibs.core.storage.S3StorageService

class S3ReadmeCRUDService(
    private val readmeProperties: ReadmeConfigurationProperties,
    private val s3StorageService: S3StorageService,
) : ReadmeCRUDService {
    private val bucketName = readmeProperties.s3.bucketName ?: throw IllegalArgumentException("Bucket name is required for S3 mode")

    override fun readReadmeRaw(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, "raw")
    }

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

    private fun readReadmeWithFallback(projectId: Int?, scmRepositoryId: Int?, type: String): String? {
        projectId?.let { id ->
            readProjectReadme(id, type)?.let { return it }
        }
        return scmRepositoryId?.let { readRepoReadme(it, type) }
    }

    private fun readProjectReadme(projectId: Int, type: String): String? =
        readReadme(getProjectS3Key(projectId, type))

    private fun readRepoReadme(scmRepositoryId: Int, type: String): String? =
        readReadme(getRepoS3Key(scmRepositoryId, type))

    private fun readReadme(key: String): String? = s3StorageService.readText(bucketName, key)

    override fun writeReadmeFiles(projectId: Int, rawContent: String, mdContent: String, htmlContent: String) {
        writeReadmeContent(projectId = projectId, type = "raw", content = rawContent)
        writeReadmeContent(projectId = projectId, type = "md", content = mdContent)
        writeReadmeContent(projectId = projectId, type = "html", content = htmlContent)
    }

   private fun writeReadmeContent(projectId: Int, type: String, content: String) {
        val key = getProjectS3Key(projectId, type)
        s3StorageService.writeText(bucketName, key, content)
    }

    private fun getProjectS3Key(projectId: Int, type: String): String {
        val fileName = getFilename(projectId, type)
        return "${readmeProperties.s3.prefix}/project/$fileName"
    }

    private fun getRepoS3Key(scmRepositoryId: Int, type: String): String {
        val fileName = getFilename(scmRepositoryId, type)
        return "${readmeProperties.s3.prefix}/$fileName"
    }

    private fun getFilename(id: Int, type: String): String {
        return when (type) {
            "raw" -> "readme-$id-raw.md"
            "md" -> "readme-$id.md"
            "html" -> "readme-$id.html"
            else -> throw IllegalArgumentException("Type can only be \"raw\", \"md\" or \"html\"")
        }
    }
}