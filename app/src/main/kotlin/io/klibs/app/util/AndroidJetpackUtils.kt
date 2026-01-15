package io.klibs.app.util

private val ANDROID_REPO_LINK_REGEX = Regex("(http://|https://)(www\\.)?developer\\.android\\.com/([^/]+)/([^/]+)/releases/([^/]+)")
private val ANDROIDX_GITHUB_OWNER = "androidx"
private val ANDROIDX_GITHUB_REPOSITORY = "androidx"

val ANDROIDX_OWNER_AND_GITHUB_REPOSITORY: Pair<String, String> = Pair(ANDROIDX_GITHUB_OWNER, ANDROIDX_GITHUB_REPOSITORY)
fun String.isAndroidxProject(): Boolean {
    return ANDROID_REPO_LINK_REGEX.matches(this)
}
