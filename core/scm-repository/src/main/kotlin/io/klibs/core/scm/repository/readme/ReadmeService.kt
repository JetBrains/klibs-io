package io.klibs.core.scm.repository.readme

interface ReadmeService {
    fun readReadmeMd(projectId: Int?, scmRepositoryId: Int?, ownerLogin: String): String?
    fun readReadmeHtml(projectId: Int?, scmRepositoryId: Int?, ownerLogin: String): String?
    fun writeReadmeFiles(projectId: Int, mdContent: String, htmlContent: String)
}
