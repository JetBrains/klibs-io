package io.klibs.core.readme.service

interface ReadmeService {
    fun readReadmeMd(projectId: Int?, scmRepositoryId: Int?): String?
    fun readReadmeHtml(projectId: Int?, scmRepositoryId: Int?): String?
    fun writeReadmeFiles(projectId: Int, mdContent: String, htmlContent: String)
}