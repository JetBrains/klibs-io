package io.klibs.core.pckg.model

import io.klibs.core.pckg.enums.PackageProcessingStatus

data class PackageStatusOverview (
    val groupId: String,
    val artifactId: String,
    val version: String,
    val status: PackageProcessingStatus,
)
