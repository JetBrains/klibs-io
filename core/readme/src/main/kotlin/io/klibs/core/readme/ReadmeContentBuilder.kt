package io.klibs.core.readme

import io.klibs.integration.github.GitHubIntegration
import org.springframework.stereotype.Service

data class GitHubIndexingReadmeContent(
    val markdown: String,
    val html: String,
    val minimized: String,
)

@Service
class ReadmeContentBuilder(
    private val gitHubIntegration: GitHubIntegration,
    private val readmeProcessors: List<ReadmeProcessor>,
) {
    fun buildFromMarkdown(
        readmeMd: String,
        nativeId: Long,
        ownerLogin: String,
        repoName: String,
        defaultBranch: String,
    ): GitHubIndexingReadmeContent {
        val readmeHtml = gitHubIntegration.markdownToHtml(readmeMd, nativeId)
            ?: error("No HTML content, even though its got Markdown readme? ghRepositoryId=$nativeId")

        val processedReadmeHtml = processReadme(readmeHtml, ownerLogin, repoName, defaultBranch, ReadmeType.HTML)
        val processedMarkdownReadme = gitHubIntegration.markdownRender(readmeMd, nativeId)
            ?.let { processReadme(it, ownerLogin, repoName, defaultBranch, ReadmeType.MARKDOWN) }
            ?: error("No Markdown content, even though its got Markdown readme? ghRepositoryId=$nativeId")

        val minimizedReadme = processReadme(readmeMd, ownerLogin, repoName, defaultBranch, ReadmeType.MINIMIZED_MARKDOWN)

        return GitHubIndexingReadmeContent(
            markdown = processedMarkdownReadme,
            html = processedReadmeHtml,
            minimized = minimizedReadme,
        )
    }

    private fun processReadme(
        readmeContent: String,
        ownerLogin: String,
        repoName: String,
        defaultBranch: String,
        type: ReadmeType,
    ) = readmeProcessors.filter { processor -> processor.isApplicable(type) }
        .fold(readmeContent) { processedReadme, processor ->
            processor.process(
                processedReadme,
                readmeOwner = ownerLogin,
                repoName,
                defaultBranch,
            )
        }
}
