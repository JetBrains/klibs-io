package io.klibs.core.project

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.project.enums.MarkerType
import java.time.Instant

data class ProjectDetails(
    val id: Int,

    val ownerType: ScmOwnerType,
    val ownerLogin: String,

    val name: String,
    val description: String?,

    val platforms: List<PackagePlatform>,

    val latestReleaseVersion: String?,
    val latestReleasePublishedAt: Instant?,

    val linkHomepage: String?,

    val hasGhPages: Boolean,
    val hasIssues: Boolean,
    val hasWiki: Boolean,

    val stars: Int,
    val createdAt: Instant,
    val openIssues: Int?,

    val licenseName: String?,

    val lastActivityAt: Instant,
    val updatedAt: Instant,

    val tags: List<String>,
    val markers: List<MarkerType>,
) {
    fun getGitHubRepositoryLink(): String {
        return "https://github.com/${this.ownerLogin}/${this.name}"
    }

    fun getGitHubPagesLink(): String? {
        return when {
            this.hasGhPages -> "https://${this.ownerLogin}.github.io/${this.name}"
            else -> null
        }
    }

    fun getIssuesLink(): String? {
        return when {
            this.hasIssues -> "https://github.com/${this.ownerLogin}/${this.name}/issues"
            else -> null
        }
    }

    fun getWikiLink(): String? {
        return when {
            this.hasWiki -> "https://github.com/${this.ownerLogin}/${this.name}/wiki"
            else -> null
        }
    }
}
