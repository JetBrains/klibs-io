package io.klibs.app.indexing

import io.klibs.app.exceptions.PackageIndexingKnownException
import io.klibs.app.service.UserRequestReportWriter
import io.klibs.core.pckg.dto.MavenCoordinateDTO
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.service.MavenCoordinateService
import io.klibs.core.pckg.service.RejectedMavenCoordinatesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PackageIndexingErrorHandler(
    private val mavenCoordinateService: MavenCoordinateService,
    private val rejectedMavenCoordinatesService: RejectedMavenCoordinatesService,
    private val userRequestReportWriter: UserRequestReportWriter,
    private val indexingRequestRepository: IndexingRequestRepository,
) {
    private val logger = LoggerFactory.getLogger(PackageIndexingErrorHandler::class.java)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(requestId: Long, exception: PackageIndexingKnownException) {
        val coordinates = exception.coordinates
        val coordinate = mavenCoordinateService.resolveOrCreate(coordinates)
        rejectedMavenCoordinatesService.save(
            coordinate, exception.scraperType, exception.scmUrl, exception.errorType,
        )
        logger.trace("Persisting the package for {}", coordinate)
        userRequestReportWriter.saveFailureReport(requestId, exception.message)
        indexingRequestRepository.deleteById(requestId)
    }
}
