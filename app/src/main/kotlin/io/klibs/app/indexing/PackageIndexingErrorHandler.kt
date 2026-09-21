package io.klibs.app.indexing

import io.klibs.app.exceptions.PackageIndexingKnownException
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.service.MavenArtifactService
import io.klibs.core.pckg.service.RejectedMavenCoordinatesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PackageIndexingErrorHandler(
    private val mavenArtifactService: MavenArtifactService,
    private val rejectedMavenCoordinatesService: RejectedMavenCoordinatesService,
    private val userRequestReportWriter: UserRequestReportWriter,
    private val indexingRequestRepository: IndexingRequestRepository,
) {
    private val logger = LoggerFactory.getLogger(PackageIndexingErrorHandler::class.java)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(requestId: Long, exception: PackageIndexingKnownException) {
        val coordinates = exception.coordinates
        val artifact = mavenArtifactService.resolveOrCreate(coordinates)
        rejectedMavenCoordinatesService.save(
            MavenArtifactDTO.fromEntity(artifact.toEntityRef()), exception.scraperType, exception.scmUrl, exception.errorType,
        )
        logger.trace("Persisting the package for {}", artifact)
        userRequestReportWriter.saveFailureReport(requestId, exception.message)
        indexingRequestRepository.deleteById(requestId)
    }
}
