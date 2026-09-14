package io.klibs.app.indexing

import io.klibs.app.exceptions.PackageIndexingKnownException
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.entity.MavenArtifactEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.MavenArtifactRepository
import io.klibs.core.pckg.service.NonKmpPackageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PackageIndexingErrorHandler(
    private val mavenArtifactRepository: MavenArtifactRepository,
    private val nonKmpPackageService: NonKmpPackageService,
    private val userRequestReportWriter: UserRequestReportWriter,
    private val indexingRequestRepository: IndexingRequestRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(requestId: Long, exception: PackageIndexingKnownException) {
        val coordinates = exception.coordinates
        val artifact = mavenArtifactRepository.findByGroupIdAndArtifactIdAndVersion(
            coordinates.groupId, coordinates.artifactId, coordinates.version,
        ) ?: mavenArtifactRepository.save(
            MavenArtifactEntity(
                groupId = coordinates.groupId, artifactId = coordinates.artifactId, version = coordinates.version,
            )
        )
        nonKmpPackageService.save(
            MavenArtifactDTO.fromEntity(artifact), exception.releaseTs, exception.scraperType, exception.scmUrl, exception.errorType,
        )
        userRequestReportWriter.saveFailureReport(requestId, exception.message)
        indexingRequestRepository.deleteById(requestId)
    }
}
