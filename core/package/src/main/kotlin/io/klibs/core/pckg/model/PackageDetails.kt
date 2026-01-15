package io.klibs.core.pckg.model

import java.time.Instant

data class PackageDetails(
    val id: Long,
    val projectId: Int?,

    val groupId: String,
    val artifactId: String,

    val version: String,
    val releasedAt: Instant,

    val name: String?,
    val description: String?,

    val targets: List<PackageTarget>,

    val licenses: List<PackageLicense>,
    val developers: List<PackageDeveloper>,

    val buildTool: String,
    val buildToolVersion: String,
    val kotlinVersion: String,

    val url: String?,
    val scmUrl: String?
)
