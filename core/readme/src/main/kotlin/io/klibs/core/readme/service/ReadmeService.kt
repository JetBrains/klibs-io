package io.klibs.core.readme.service

import io.klibs.core.readme.AndroidxReadmeProvider
import io.klibs.core.readme.entity.ReadmeMetadataEntity
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReadmeService(
    private val s3ReadmeService: S3ReadmeCRUDService,
    private val androidxReadmeProvider: AndroidxReadmeProvider,
    private val readmeMetadataRepository: ReadmeMetadataRepository,
) {
    data class ProjectInfo(
        val id: Int?,
        val scmRepositoryId: Int?,
        val name: String,
        val ownerLogin: String,
    )

    fun readReadmeMd(projectInfo: ProjectInfo): String? =
        if (projectInfo.ownerLogin == AndroidxReadmeProvider.OWNER_NAME) {
            androidxReadmeProvider.resolve(projectInfo.name, "md")
        } else {
            s3ReadmeService.readReadmeMd(projectInfo.id, projectInfo.scmRepositoryId)
        }

    fun readReadmeHtml(projectInfo: ProjectInfo): String? =
        if (projectInfo.ownerLogin == AndroidxReadmeProvider.OWNER_NAME) {
            androidxReadmeProvider.resolve(projectInfo.name, "html")
        } else {
            s3ReadmeService.readReadmeHtml(projectInfo.id, projectInfo.scmRepositoryId)
        }

    fun readReadmeRaw(projectInfo: ProjectInfo): String? =
        if (projectInfo.ownerLogin == AndroidxReadmeProvider.OWNER_NAME) {
            androidxReadmeProvider.resolve(projectInfo.name, "md")
        } else {
            s3ReadmeService.readReadmeRaw(projectInfo.id, projectInfo.scmRepositoryId)
        }

    @Transactional
    fun writeReadmeFiles(
        projectId: Int,
        scmRepositoryId: Int,
        readmeMetadataEntity: ReadmeMetadataEntity? = null,
        rawContent: String,
        mdContent: String,
        htmlContent: String
    ) {
        saveReadmeMetadata(scmRepositoryId, readmeMetadataEntity)

        s3ReadmeService.writeReadmeFiles(projectId, rawContent, mdContent, htmlContent)
    }

    private fun saveReadmeMetadata(
        scmRepositoryId: Int,
        readmeMetadataEntity: ReadmeMetadataEntity?,
    ) {
        val metadataToSave = (
                readmeMetadataEntity
                    ?: readmeMetadataRepository.findByScmRepoId(scmRepositoryId)
                    ?: ReadmeMetadataEntity(scmRepoId = scmRepositoryId)
                ).apply {
                lastSyncedAt = Instant.now()
                lastProcessedAt = Instant.now()
            }

        readmeMetadataRepository.save(metadataToSave)
    }
}