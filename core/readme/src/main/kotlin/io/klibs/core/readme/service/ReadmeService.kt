package io.klibs.core.readme.service

import io.klibs.core.readme.entity.ReadmeMetadataEntity
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReadmeService(
    private val s3ReadmeService: S3ReadmeCRUDService,
    private val readmeMetadataRepository: ReadmeMetadataRepository,
) {
    data class ProjectInfo(
        val id: Int?,
        val scmRepositoryId: Int? = null
    )

    fun readReadmeMd(projectInfo: ProjectInfo): String? =
            s3ReadmeService.readReadmeMd(projectInfo.id, projectInfo.scmRepositoryId)

    fun readReadmeHtml(projectInfo: ProjectInfo): String? =
            s3ReadmeService.readReadmeHtml(projectInfo.id, projectInfo.scmRepositoryId)

    fun readReadmeRaw(projectInfo: ProjectInfo): String? =
            s3ReadmeService.readReadmeRaw(projectInfo.id, projectInfo.scmRepositoryId)

    @Transactional
    fun writeReadmeFiles(
        projectId: Int,
        rawContent: String,
        mdContent: String,
        htmlContent: String
    ) {
        s3ReadmeService.writeReadmeFiles(projectId, rawContent, mdContent, htmlContent)
    }

    @Transactional
    fun saveReadmeMetadata(
        scmRepositoryId: Int,
        readmeMetadataEntity: ReadmeMetadataEntity? = null,
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