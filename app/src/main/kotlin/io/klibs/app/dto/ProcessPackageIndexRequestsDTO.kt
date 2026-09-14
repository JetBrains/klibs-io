package io.klibs.app.dto

import io.klibs.app.enums.ProcessPackageIndexAction
import io.klibs.core.pckg.enums.PackageIndexingErrorType

data class ProcessPackageIndexRequestsDTO(
    val action: ProcessPackageIndexAction,
    val packages: List<PackageCoordinatesDTO>,
    val errorType: PackageIndexingErrorType? = PackageIndexingErrorType.MISSING_TOOLING_METADATA,
    val reason: String? = null,
)
