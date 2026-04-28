package io.klibs.core.readme.impl

import io.klibs.core.readme.ReadmeProcessor
import io.klibs.core.readme.service.GithubLfsDetector
import java.net.URI
import java.net.URISyntaxException

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
            hrefUrlBuilder = { link ->
                "$GITHUB_URL/$readmeOwner/$readmeRepositoryName/blob/$repositoryDefaultBranch/$link"
            },
            srcUrlBuilder = { link ->
                resolveLfsUrl("$GITHUB_RAW_CONTENT_BASE_URL/$readmeOwner/$readmeRepositoryName/$repositoryDefaultBranch/$link")
            },
        )
    }

    protected fun replaceRelativeLinks(
        readmeContent: String,
        hrefUrlBuilder: (String) -> String,
        srcUrlBuilder: (String) -> String,
    ): String {
        return readmeContent
            .replaceFirstGroupValue(hrefRelativeLinkRegex) { link -> "href=\"${hrefUrlBuilder(link)}\"" }
            .replaceFirstGroupValue(rawContentRegex) { link -> "src=\"${srcUrlBuilder(link)}\"" }
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
