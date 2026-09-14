package io.klibs.app.indexing

import io.klibs.app.api.ProcessPackageIndexRequestsResponse
import io.klibs.app.dto.PackageCoordinatesDTO
import io.klibs.app.dto.ProcessPackageIndexRequestsDTO
import io.klibs.app.dto.ProcessPackageIndexResultDTO
import io.klibs.app.enums.ProcessPackageIndexAction
import io.klibs.app.enums.ProcessResultStatus
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.entity.MavenArtifactEntity
import io.klibs.core.pckg.entity.NonKmpPackageEntity
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.MavenArtifactRepository
import io.klibs.core.pckg.repository.NonKmpPackageRepository
import io.klibs.integration.maven.ScraperType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManualPackageIndexProcessingService(
    private val indexingRequestRepository: IndexingRequestRepository,
    private val mavenArtifactRepository: MavenArtifactRepository,
    private val nonKmpPackageRepository: NonKmpPackageRepository,
    private val userRequestReportWriter: UserRequestReportWriter,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ManualPackageIndexProcessingService::class.java)
    }

    @Transactional
    fun process(request: ProcessPackageIndexRequestsDTO): ProcessPackageIndexRequestsResponse {
        val results = request.packages.map { pkg ->
            try {
                when (request.action) {
                    ProcessPackageIndexAction.REMOVE -> processRemove(pkg, request.reason)
                    ProcessPackageIndexAction.MARK_NON_KMP -> processMarkNonKmp(pkg, request.errorType, request.reason)
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to process coordinate {}:{}:{} with action {}",
                    pkg.groupId,
                    pkg.artifactId,
                    pkg.version,
                    request.action,
                    e,
                )
                ProcessPackageIndexResultDTO(
                    groupId = pkg.groupId,
                    artifactId = pkg.artifactId,
                    version = pkg.version,
                    status = ProcessResultStatus.ERROR,
                    action = request.action,
                    message = e.message ?: "Unexpected error occurred",
                    affectedRequestsCount = 0,
                )
            }
        }

        return ProcessPackageIndexRequestsResponse(
            totalProcessed = results.size,
            results = results,
        )
    }

    private fun processRemove(
        pkg: PackageCoordinatesDTO,
        reason: String?,
    ): ProcessPackageIndexResultDTO {
        val foundPackageIndexRequests = if (pkg.version != null) {
            indexingRequestRepository.findAllByGroupIdAndArtifactIdAndVersion(pkg.groupId, pkg.artifactId, pkg.version)
        } else {
            indexingRequestRepository.findAllByGroupIdAndArtifactId(pkg.groupId, pkg.artifactId)
        }

        if (foundPackageIndexRequests.isEmpty()) {
            logger.info(
                "Remove requested for {}:{}:{} but no matching queue records found",
                pkg.groupId, pkg.artifactId, pkg.version
            )
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

        logger.info(
            "Successfully removed {} request(s) for {}:{}:{}",
            foundPackageIndexRequests.size, pkg.groupId, pkg.artifactId, pkg.version
        )

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

    private fun processMarkNonKmp(
        pkg: PackageCoordinatesDTO,
        errorType: PackageIndexingErrorType?,
        reason: String?,
    ): ProcessPackageIndexResultDTO {
        val effectiveErrorType =
            errorType ?: throw IllegalArgumentException("Error type must be specified for non-KMP package processing")

        return if (pkg.version != null) {
            processSingleNonKmpPackage(pkg, pkg.version, effectiveErrorType, reason)
        } else {
            processMultipleNonKmpPackages(pkg, effectiveErrorType, reason)
        }
    }

    private fun processMultipleNonKmpPackages(
        packageCoordinatesDTO: PackageCoordinatesDTO,
        effectiveErrorType: PackageIndexingErrorType,
        reason: String?,
    ): ProcessPackageIndexResultDTO {
        val foundPackageIndexRequests = indexingRequestRepository.findAllByGroupIdAndArtifactId(
            packageCoordinatesDTO.groupId,
            packageCoordinatesDTO.artifactId,
        )

        if (foundPackageIndexRequests.isEmpty()) {
            logger.warn(
                "Cannot mark {}:{} as non-KMP without version when no queue records exist",
                packageCoordinatesDTO.groupId,
                packageCoordinatesDTO.artifactId,
            )
            return ProcessPackageIndexResultDTO(
                groupId = packageCoordinatesDTO.groupId,
                artifactId = packageCoordinatesDTO.artifactId,
                version = null,
                status = ProcessResultStatus.ERROR,
                action = ProcessPackageIndexAction.MARK_NON_KMP,
                message = "Version is required when no matching index requests exist in queue",
                affectedRequestsCount = 0,
            )
        }

        val versions = foundPackageIndexRequests.mapNotNull { it.version }.distinct()
        if (versions.isEmpty()) {
            logger.warn(
                "Matching queue requests for {}:{} do not specify a version",
                packageCoordinatesDTO.groupId,
                packageCoordinatesDTO.artifactId,
            )
            return ProcessPackageIndexResultDTO(
                groupId = packageCoordinatesDTO.groupId,
                artifactId = packageCoordinatesDTO.artifactId,
                version = null,
                status = ProcessResultStatus.ERROR,
                action = ProcessPackageIndexAction.MARK_NON_KMP,
                message = "Matching requests do not specify a version; version is required to mark as non-KMP",
                affectedRequestsCount = 0,
            )
        }

        saveNonKmpPackagesAndCleanQueue(
            packageCoordinatesDTO,
            versions,
            foundPackageIndexRequests,
            effectiveErrorType,
            reason
        )

        logger.info(
            "Marked {}:{} as non-KMP for {} version(s); affected {} queue request(s)",
            packageCoordinatesDTO.groupId,
            packageCoordinatesDTO.artifactId,
            versions.size,
            foundPackageIndexRequests.size
        )

        return ProcessPackageIndexResultDTO(
            groupId = packageCoordinatesDTO.groupId,
            artifactId = packageCoordinatesDTO.artifactId,
            version = null,
            status = ProcessResultStatus.SUCCESS,
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            message = "Successfully marked as non-KMP for ${versions.size} version(s); affected ${foundPackageIndexRequests.size} request(s)",
            affectedRequestsCount = foundPackageIndexRequests.size,
        )
    }

    private fun processSingleNonKmpPackage(
        pkg: PackageCoordinatesDTO,
        version: String,
        effectiveErrorType: PackageIndexingErrorType,
        reason: String?,
    ): ProcessPackageIndexResultDTO {
        val matchingRequests =
            indexingRequestRepository.findAllByGroupIdAndArtifactIdAndVersion(pkg.groupId, pkg.artifactId, version)

        saveNonKmpPackagesAndCleanQueue(pkg, listOf(version), matchingRequests, effectiveErrorType, reason)

        logger.info(
            "Marked {}:{}:{} as non-KMP; affected {} queue request(s)",
            pkg.groupId, pkg.artifactId, version, matchingRequests.size,
        )

        return ProcessPackageIndexResultDTO(
            groupId = pkg.groupId,
            artifactId = pkg.artifactId,
            version = version,
            status = ProcessResultStatus.SUCCESS,
            action = ProcessPackageIndexAction.MARK_NON_KMP,
            message = "Successfully marked as non-KMP; affected ${matchingRequests.size} request(s)",
            affectedRequestsCount = matchingRequests.size,
        )
    }

    private fun saveNonKmpPackagesAndCleanQueue(
        packageCoordinatesDto: PackageCoordinatesDTO,
        versions: List<String>,
        foundPackageIndexRequests: List<IndexingRequestEntity>,
        effectiveErrorType: PackageIndexingErrorType,
        reason: String?,
    ) {
        versions.forEach { version ->
            val scraperType =
                foundPackageIndexRequests.firstOrNull { it.version == version }?.repo ?: ScraperType.CENTRAL_SONATYPE

            saveNonKmpPackage(
                packageCoordinatesDto.groupId,
                packageCoordinatesDto.artifactId,
                version,
                scraperType,
                effectiveErrorType
            )
        }

        deleteRequestsAndReportFailure(
            foundPackageIndexRequests,
            reason ?: "Marked as non-KMP manually by administrator",
        )
    }

    private fun saveNonKmpPackage(
        groupId: String,
        artifactId: String,
        version: String,
        repo: ScraperType,
        errorType: PackageIndexingErrorType,
    ) {
        val artifact =
            mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
                ?: mavenArtifactRepository.save(
                    MavenArtifactEntity(groupId = groupId, artifactId = artifactId, version = version),
                )

        val internalArtifactId = requireNotNull(artifact.id)
        if (!nonKmpPackageRepository.existsByMavenArtifactId(internalArtifactId)) {
            nonKmpPackageRepository.save(
                NonKmpPackageEntity(mavenArtifact = artifact, repo = repo, scmUrl = null, errorType = errorType),
            )
        }
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
