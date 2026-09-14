package io.klibs.app.api

import io.klibs.app.dto.PackageCoordinatesDTO
import io.klibs.app.dto.ProcessPackageIndexRequestsDTO
import io.klibs.app.enums.ProcessPackageIndexAction
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request payload for manual processing of package index requests")
data class ProcessPackageIndexRequestsRequest(
    val action: ProcessPackageIndexAction,
    val packages: List<PackageCoordinates>,
    val errorType: PackageIndexingErrorType? = PackageIndexingErrorType.MISSING_TOOLING_METADATA,
    val reason: String? = null,
) {
    @Schema(description = "Target Maven package coordinates")
    data class PackageCoordinates(
        val groupId: String,
        val artifactId: String,
        val version: String? = null,
    ) {
        fun toDTO(): PackageCoordinatesDTO = PackageCoordinatesDTO(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
        )
    }

    fun toDTO(): ProcessPackageIndexRequestsDTO = ProcessPackageIndexRequestsDTO(
        action = action,
        packages = packages.map { it.toDTO() },
        errorType = errorType,
        reason = reason,
    )
}

