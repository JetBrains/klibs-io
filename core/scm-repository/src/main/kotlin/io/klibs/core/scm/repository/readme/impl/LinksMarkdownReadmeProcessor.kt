package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class LinksMarkdownReadmeProcessor : LinksBaseReadmeProcessor() {

    private val markdownInternalLinkPattern = Regex("""(!?\[.*?]\()(?!https?://|#)([^)]+)\)""")

    override fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        val repositoryBaseUrl = repositoryBaseUrl(readmeOwner, readmeRepositoryName, repositoryDefaultBranch)
        val repositoryRawUrl = repositoryRawUrl(readmeOwner, readmeRepositoryName, repositoryDefaultBranch)
        return super.process(
            replaceRelativeMarkdownLinks(readmeContent, repositoryRawUrl, repositoryBaseUrl),
            readmeOwner,
            readmeRepositoryName,
            repositoryDefaultBranch
        )
    }

    override fun isApplicable(type: ReadmeType): Boolean {
        return type == ReadmeType.MARKDOWN
    }

    private fun repositoryBaseUrl(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "$GITHUB_URL/${readmeOwner}/${readmeRepositoryName}/blob/${repositoryDefaultBranch}"
    }

    private fun repositoryRawUrl(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "$GITHUB_RAW_CONTENT_BASE_URL/$readmeOwner/$readmeRepositoryName/${repositoryDefaultBranch}"
    }

    private fun replaceRelativeMarkdownLinks(
        readmeContent: String,
        repoUrlForRawContent: String,
        repoUrl: String
    ): String {
        return markdownInternalLinkPattern.replace(readmeContent) { matchResult ->
            val prefix = matchResult.groupValues[1] // e.g., "![Quick start](" or "[Quick start]("
            val relativePath = matchResult.groupValues[2] // e.g., "docs/readme.md"

            if (relativePath.isImageFile()) {
                "$prefix$repoUrlForRawContent/$relativePath)"
            } else {
                "$prefix$repoUrl/$relativePath)"
            }
        }
    }
}

private fun String.isImageFile(): Boolean {
    val imageRegex = Regex("\\.(jpg|jpeg|png|gif|bmp|tiff|webp|svg|heic|ico)$", RegexOption.IGNORE_CASE)
    return imageRegex.containsMatchIn(this.lowercase())
}