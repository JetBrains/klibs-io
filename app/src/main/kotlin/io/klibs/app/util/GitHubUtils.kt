package io.klibs.app.util

private val GITHUB_REPO_LINK_REGEX = Regex("(http://|https://)(www\\.)?github\\.com/([^/]+)/([^/]+?)(\\.git)?(/|\$)")

/**
 * @return owner to repo name
 */
fun parseGitHubLink(link: String): Pair<String, String>? {
    val groupValues = GITHUB_REPO_LINK_REGEX.find(link)?.groupValues ?: return null

    val owner = groupValues[3].takeIf { it.isNotBlank() } ?: return null
    val repo = groupValues[4].takeIf { it.isNotBlank() } ?: return null
    return owner to repo
}

fun normalizeGitHubLink(link: String): String {
    val (owner, repo) = parseGitHubLink(link) ?: return link
    return "https://github.com/${owner}/${repo}"
}
