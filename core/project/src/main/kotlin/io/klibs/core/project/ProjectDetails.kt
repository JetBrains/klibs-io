package io.klibs.core.project

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.project.enums.MarkerType
import java.time.Instant

data class ProjectDetails(
    val id: Int,

    val ownerType: ScmOwnerType,
    val ownerLogin: String,
    val repoName: String,

    val name: String,
    val description: String?,

    val platforms: List<PackagePlatform>,

    val latestReleaseVersion: String?,
    val latestReleasePublishedAt: Instant?,

    val linkHomepage: String?,

    val hasGhPages: Boolean,
    val hasIssues: Boolean,
    val hasWiki: Boolean,
    val hasReadme: Boolean,

    val stars: Int,
    val createdAt: Instant,
    val openIssues: Int?,

    val licenseName: String?,

    val lastActivityAt: Instant,
    val updatedAt: Instant,

    val tags: List<String>,
    val markers: List<MarkerType>,
) {
    private val isAndroidx: Boolean get() = ownerLogin == ANDROIDX_OWNER

    fun getGitHubRepositoryLink(): String {
        return if (isAndroidx) {
            "https://github.com/$ANDROIDX_OWNER/$ANDROIDX_OWNER/tree/androidx-main/${this.name}"
        } else {
            "https://github.com/${this.ownerLogin}/${this.repoName}"
        }
    }

    fun getGitHubPagesLink(): String? {
        if (isAndroidx) return null
        return when {
            this.hasGhPages -> "https://${this.ownerLogin}.github.io/${this.repoName}"
            else -> null
        }
    }

    fun getHomepageLink(): String? {
        return when {
            isAndroidx -> "https://developer.android.com/jetpack/androidx/releases/${this.name}"
            else -> linkHomepage
        }
    }

    fun getIssuesLink(): String? {
        return when {
            this.hasIssues -> "https://github.com/${this.ownerLogin}/${this.repoName}/issues"
            else -> null
        }
    }

    fun getWikiLink(): String? {
        return when {
            this.hasWiki -> "https://github.com/${this.ownerLogin}/${this.repoName}/wiki"
            else -> null
        }
    }

    private companion object {
        private const val ANDROIDX_OWNER = "androidx"
    }
}
