package io.klibs.core.readme.service

import io.klibs.core.readme.AndroidxReadmeProvider
import org.springframework.stereotype.Service

@Service
class ReadmeServiceDispatcher(
    private val s3ReadmeService: S3ReadmeService,
    private val androidxReadmeProvider: AndroidxReadmeProvider,
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

    fun writeReadmeFiles(projectId: Int, mdContent: String, htmlContent: String) {
        s3ReadmeService.writeReadmeFiles(projectId, mdContent, htmlContent)
    }
}