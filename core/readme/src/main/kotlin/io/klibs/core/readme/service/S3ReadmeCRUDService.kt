package io.klibs.core.readme.service

import io.klibs.core.readme.ReadmeConfigurationProperties
import io.klibs.core.readme.ReadmeType
import io.klibs.core.storage.S3StorageService
import org.springframework.stereotype.Service

@Service
class S3ReadmeCRUDService(
    private val readmeProperties: ReadmeConfigurationProperties,
    private val s3StorageService: S3StorageService,
) : ReadmeCRUDService {
    private val bucketName = readmeProperties.s3.bucketName ?: throw IllegalArgumentException("Bucket name is required for S3 mode")

    override fun readReadmeRaw(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, ReadmeType.RAW)
    }

    override fun readReadmeMd(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, ReadmeType.MARKDOWN)
    }

    override fun readReadmeHtml(projectId: Int?, scmRepositoryId: Int?): String? {
        require(projectId != null || scmRepositoryId != null) {
            "Either projectId or scmRepositoryId must be provided"
        }
        return readReadmeWithFallback(projectId, scmRepositoryId, ReadmeType.HTML)
    }

    private fun readReadmeWithFallback(projectId: Int?, scmRepositoryId: Int?, type: ReadmeType): String? {
        projectId?.let { id ->
            readProjectReadme(id, type)?.let { return it }
        }
        return scmRepositoryId?.let { readRepoReadme(it, type) }
    }

    private fun readProjectReadme(projectId: Int, type: ReadmeType): String? =
        readReadme(getProjectS3Key(projectId, type))

    private fun readRepoReadme(scmRepositoryId: Int, type: ReadmeType): String? =
        readReadme(getRepoS3Key(scmRepositoryId, type))

    private fun readReadme(key: String): String? = s3StorageService.readText(bucketName, key)

    override fun writeReadmeFiles(projectId: Int, rawContent: String, mdContent: String, htmlContent: String) {
        writeReadmeContent(projectId = projectId, type = ReadmeType.RAW, content = rawContent)
        writeReadmeContent(projectId = projectId, type = ReadmeType.MARKDOWN, content = mdContent)
        writeReadmeContent(projectId = projectId, type = ReadmeType.HTML, content = htmlContent)
    }

   private fun writeReadmeContent(projectId: Int, type: ReadmeType, content: String) {
        val key = getProjectS3Key(projectId, type)
        s3StorageService.writeText(bucketName, key, content)
    }

    private fun getProjectS3Key(projectId: Int, type: ReadmeType): String {
        val fileName = getFilename(projectId, type)
        return "${readmeProperties.s3.prefix}/project/$fileName"
    }

    private fun getRepoS3Key(scmRepositoryId: Int, type: ReadmeType): String {
        val fileName = getFilename(scmRepositoryId, type)
        return "${readmeProperties.s3.prefix}/$fileName"
    }

    private fun getFilename(id: Int, type: ReadmeType): String {
        return when (type) {
            ReadmeType.RAW -> "readme-$id-raw.md"
            ReadmeType.MARKDOWN -> "readme-$id.md"
            ReadmeType.HTML -> "readme-$id.html"
            else -> throw IllegalArgumentException("Type can only be \"raw\", \"md\" or \"html\"")
        }
    }
}