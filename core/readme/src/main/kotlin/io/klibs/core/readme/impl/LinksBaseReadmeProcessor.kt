package io.klibs.core.readme.impl

import io.klibs.core.readme.ReadmeProcessor
import io.klibs.core.readme.service.GithubLfsDetector
import java.net.URI
import java.net.URISyntaxException

private const val ORIGINAL_PATH_PARAMETER_NAME = "<original_path>"

abstract class LinksBaseReadmeProcessor(
    protected val lfsDetector: GithubLfsDetector
) : ReadmeProcessor {
    protected val GITHUB_URL = "https://github.com"
    protected val GITHUB_RAW_CONTENT_BASE_URL = "https://raw.githubusercontent.com"
    protected val GITHUB_LFS_CONTENT_BASE_URL = "https://media.githubusercontent.com/media"
    private val rawContentRegex = Regex("src=\"(?!https?://|#)([^\"]*)\"")
    private val hrefRelativeLinkRegex = Regex("href=\"(?!https?://|#)([^\"]*)\"")

    override fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return replaceRelativeLinks(
            readmeContent,
            constructNewHrefUrlPrefix(readmeOwner, readmeRepositoryName, repositoryDefaultBranch),
            constructNewSrcUrlPrefix(readmeOwner, readmeRepositoryName, repositoryDefaultBranch)
        )
    }

    protected fun replaceRelativeLinks(
        readmeContent: String,
        hrefUrlPrefix: String,
        srcUrlPrefix: String
    ): String {
        return readmeContent.replaceFirstGroupValue(hrefRelativeLinkRegex) { link ->
            val rawUrl = hrefUrlPrefix.replace(ORIGINAL_PATH_PARAMETER_NAME, link)
            "href=\"$rawUrl\""
        }.replaceFirstGroupValue(rawContentRegex) { link ->
            val rawUrl = srcUrlPrefix.replace(ORIGINAL_PATH_PARAMETER_NAME, link)
            "src=\"${resolveLfsUrl(rawUrl)}\""
        }
    }

    private fun constructNewSrcUrlPrefix(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "$GITHUB_RAW_CONTENT_BASE_URL/$readmeOwner/$readmeRepositoryName/${repositoryDefaultBranch}/$ORIGINAL_PATH_PARAMETER_NAME"
    }

    private fun constructNewHrefUrlPrefix(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "$GITHUB_URL/${readmeOwner}/${readmeRepositoryName}/blob/${repositoryDefaultBranch}/$ORIGINAL_PATH_PARAMETER_NAME"
    }

    private fun String.replaceFirstGroupValue(regex: Regex, replace: (match: String) -> String): String {
        return replace(regex) { matchResult ->
            val link = matchResult.groups[1]?.value ?: return@replace matchResult.value
            if (parseLink(link)?.isAbsolute == true) {
                matchResult.value
            } else {
                replace(link)
            }
        }
    }

    protected fun resolveLfsUrl(rawUrl: String): String {
        return if (lfsDetector.isLfsFile(rawUrl)) {
            rawUrl.replace(GITHUB_RAW_CONTENT_BASE_URL, GITHUB_LFS_CONTENT_BASE_URL)
        } else {
            rawUrl
        }
    }

    private fun parseLink(link: String): URI? {
        return try {
            URI(link)
        } catch (e: URISyntaxException) {
            null
        }
    }
}