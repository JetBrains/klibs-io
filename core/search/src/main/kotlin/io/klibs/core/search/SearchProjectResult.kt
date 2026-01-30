package io.klibs.core.search

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import java.time.Instant

data class SearchProjectResult(
    val id: Int,
    val name: String,
    val repoName: String,
    val description: String?,

    val vcsStars: Int,

    val ownerType: ScmOwnerType,
    val ownerLogin: String,

    val licenseName: String?,

    val latestVersion: String,
    val latestVersionPublishedAt: Instant,

    val platforms: List<PackagePlatform>,
    val targets: List<String> = emptyList(),

    val tags: List<String>,
    val markers: List<String>
)
