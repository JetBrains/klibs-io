package io.klibs.core.pckg.dto

import io.klibs.core.pckg.enums.PackageProcessingStatus

data class PackageStatusDTO (
    val groupId: String,
    val artifactId: String,
    val version: String,
    val status: PackageProcessingStatus,
)
