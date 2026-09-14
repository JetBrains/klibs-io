package io.klibs.app.indexing

import io.klibs.app.dto.PackageCoordinatesDTO
import io.klibs.app.dto.ProcessPackageIndexRequestsDTO
import io.klibs.app.dto.ProcessPackageIndexResultDTO
import io.klibs.app.enums.ProcessPackageIndexAction
import io.klibs.app.enums.ProcessResultStatus
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.MavenCoordinateEntity
import io.klibs.core.pckg.entity.RejectedMavenCoordinateEntity
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.MavenCoordinateRepository
import io.klibs.core.pckg.repository.RejectedMavenCoordinateRepository
import io.klibs.integration.maven.ScraperType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ManualPackageIndexProcessor(
    private val mavenArtifactRepository: MavenCoordinateRepository,
    private val rejectedMavenCoordinateRepository: RejectedMavenCoordinateRepository,
    private val indexingRequestRepository: IndexingRequestRepository,
    private val userRequestReportWriter: UserRequestReportWriter,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(request: ProcessPackageIndexRequestsDTO, pkg: PackageCoordinatesDTO): ProcessPackageIndexResultDTO =
        when (request.action) {
            ProcessPackageIndexAction.REMOVE -> processRemove(pkg, request.reason)
            ProcessPackageIndexAction.REJECT -> processMarkRejectedCoordinate(pkg, request.errorType, request.reason)
        }.copy(action = request.action)

    private fun processRemove(pkg: PackageCoordinatesDTO, reason: String?): ProcessPackageIndexResultDTO {
        val foundPackageIndexRequests = findRequests(pkg)
        if (foundPackageIndexRequests.isEmpty()) {
            return ProcessPackageIndexResultDTO(
                groupId = pkg.groupId,
                artifactId = pkg.artifactId,
                version = pkg.version,
                status = ProcessResultStatus.NOT_FOUND,
                action = ProcessPackageIndexAction.REMOVE,
                message = "No matching package index requests found",
                affectedRequestsCount = 0,
            )
        }
        deleteRequestsAndReportFailure(foundPackageIndexRequests, reason ?: "Removed manually by administrator")
        return ProcessPackageIndexResultDTO(
            groupId = pkg.groupId,
            artifactId = pkg.artifactId,
            version = pkg.version,
            status = ProcessResultStatus.SUCCESS,
            action = ProcessPackageIndexAction.REMOVE,
            message = "Successfully removed ${foundPackageIndexRequests.size} request(s)",
            affectedRequestsCount = foundPackageIndexRequests.size,
        )
    }

    private fun processMarkRejectedCoordinate(
        pkg: PackageCoordinatesDTO,
        errorType: PackageIndexingErrorType?,
        reason: String?,
    ): ProcessPackageIndexResultDTO {
        val effectiveErrorType = requireNotNull(errorType) { "Error type must be specified for coordinate rejection" }
        val candidates = indexingRequestRepository.findAllByGroupIdAndArtifactId(pkg.groupId, pkg.artifactId)
        require(candidates.none { it.version == null }) {
            "Matching requests do not specify a version; resolve versionless requests before rejecting"
        }
        val foundPackageIndexRequests = candidates.filter { pkg.version == null || it.version == pkg.version }
        val versions = pkg.version?.let { listOf(it) } ?: run {
            require(foundPackageIndexRequests.isNotEmpty()) {
                "Version is required when no matching index requests exist in queue"
            }
            foundPackageIndexRequests.map {
                requireNotNull(it.version) { "Matching requests do not specify a version; version is required to reject" }
            }.distinct()
        }
        versions.forEach { version ->
            val scraperType = foundPackageIndexRequests.firstOrNull { it.version == version }?.repo
            saveNonKmpPackage(pkg.groupId, pkg.artifactId, version, scraperType, effectiveErrorType)
        }
        deleteRequestsAndReportFailure(foundPackageIndexRequests, reason ?: "Rejected manually by administrator")
        return ProcessPackageIndexResultDTO(
            groupId = pkg.groupId,
            artifactId = pkg.artifactId,
            version = pkg.version,
            status = ProcessResultStatus.SUCCESS,
            action = ProcessPackageIndexAction.REJECT,
            message = "Rejected ${versions.size} version(s); affected ${foundPackageIndexRequests.size} request(s)",
            affectedRequestsCount = foundPackageIndexRequests.size,
        )
    }

    private fun findRequests(pkg: PackageCoordinatesDTO): List<IndexingRequestEntity> =
        if (pkg.version != null) {
            indexingRequestRepository.findAllByGroupIdAndArtifactIdAndVersion(pkg.groupId, pkg.artifactId, pkg.version)
        } else {
            indexingRequestRepository.findAllByGroupIdAndArtifactId(pkg.groupId, pkg.artifactId)
        }

    private fun saveNonKmpPackage(
        groupId: String,
        artifactId: String,
        version: String,
        scraperType: ScraperType?,
        errorType: PackageIndexingErrorType,
    ) {
        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
        if (artifact != null && rejectedMavenCoordinateRepository.existsByMavenCoordinateId(requireNotNull(artifact.id))) return
        requireNotNull(scraperType) { "Repository is unknown for $groupId:$artifactId:$version; a queue request is required" }
        val coordinate = artifact ?: mavenArtifactRepository.save(
            MavenCoordinateEntity(groupId = groupId, artifactId = artifactId, version = version),
        )
        rejectedMavenCoordinateRepository.save(
            RejectedMavenCoordinateEntity(mavenCoordinate = coordinate, repo = scraperType, scmUrl = null, errorType = errorType),
        )
    }

    private fun deleteRequestsAndReportFailure(
        packageIndexRequestsToDelete: List<IndexingRequestEntity>,
        failureReason: String,
    ) {
        packageIndexRequestsToDelete.forEach { req ->
            userRequestReportWriter.saveFailureReport(req.idNotNull, failureReason)
            indexingRequestRepository.delete(req)
        }
    }
}
