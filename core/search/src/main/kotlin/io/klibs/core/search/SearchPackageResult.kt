package io.klibs.core.search

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.model.TargetGroup
import java.time.Instant

data class SearchPackageResult(
    val groupId: String,
    val artifactId: String,
    val description: String?,

    val ownerType: ScmOwnerType,
    val ownerLogin: String,

    val licenseName: String?,

    val latestVersion: String,
    val releaseTs: Instant,

    val platforms: List<PackagePlatform>,

    // TODO KTL-2556 remove once deprecated on frontend
    val targetsList: List<PackageTarget> = emptyList(),

    val targetsMap: Map<TargetGroup, Set<String>>
)
