package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeProcessor
import java.net.URI
import java.net.URISyntaxException

private const val ORIGINAL_PATH_PARAMETER_NAME = "<original_path>"

abstract class LinksBaseReadmeProcessor : ReadmeProcessor {
    protected val GITHUB_URL = "https://github.com"
    protected val GITHUB_RAW_CONTENT_BASE_URL = "https://raw.githubusercontent.com"
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
            hrefUrlPrefix.replace(ORIGINAL_PATH_PARAMETER_NAME, link)
        }.replaceFirstGroupValue(rawContentRegex) { link ->
            srcUrlPrefix.replace(ORIGINAL_PATH_PARAMETER_NAME, link)
        }
    }

    private fun constructNewSrcUrlPrefix(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "src=\"$GITHUB_RAW_CONTENT_BASE_URL/$readmeOwner/$readmeRepositoryName/${repositoryDefaultBranch}/$ORIGINAL_PATH_PARAMETER_NAME\""
    }

    private fun constructNewHrefUrlPrefix(
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return "href=\"$GITHUB_URL/${readmeOwner}/${readmeRepositoryName}/blob/${repositoryDefaultBranch}/$ORIGINAL_PATH_PARAMETER_NAME\""
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

    private fun parseLink(link: String): URI? {
        return try {
            URI(link)
        } catch (e: URISyntaxException) {
            null
        }
    }
}