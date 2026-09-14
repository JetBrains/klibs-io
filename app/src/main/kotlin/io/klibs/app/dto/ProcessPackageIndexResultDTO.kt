package io.klibs.app.dto

import io.klibs.app.enums.ProcessResultStatus
import io.klibs.app.enums.ProcessPackageIndexAction

data class ProcessPackageIndexResultDTO(
    val groupId: String,
    val artifactId: String,
    val version: String?,
    val status: ProcessResultStatus,
    val action: ProcessPackageIndexAction,
    val message: String? = null,
    val affectedRequestsCount: Int = 0,
)