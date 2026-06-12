package io.klibs.core.pckg.model

import java.time.Instant

data class PackageOverview(
    val id: Long,

    val groupId: String,
    val artifactId: String,
    val version: String,
    val latestStableVersion: String?,
    val releasedAt: Instant,

    val description: String?,

    val dependentCount: Int = 0,

    val targets: List<PackageTarget>
)