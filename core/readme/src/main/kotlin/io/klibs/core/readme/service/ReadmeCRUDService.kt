package io.klibs.core.readme.service

interface ReadmeCRUDService {
    fun readReadmeRaw(projectId: Int?, scmRepositoryId: Int?): String?
    fun readReadmeMd(projectId: Int?, scmRepositoryId: Int?): String?
    fun readReadmeHtml(projectId: Int?, scmRepositoryId: Int?): String?
    fun writeReadmeFiles(projectId: Int, rawContent: String, mdContent: String, htmlContent: String)
}