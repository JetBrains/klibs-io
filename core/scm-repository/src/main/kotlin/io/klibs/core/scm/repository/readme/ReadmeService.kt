package io.klibs.core.scm.repository.readme

interface ReadmeService {
    fun readReadmeMd(scmRepositoryId: Int): String?
    fun readReadmeHtml(scmRepositoryId: Int): String?
    fun writeReadmeFiles(scmRepositoryId: Int, mdContent: String, htmlContent: String)
}
