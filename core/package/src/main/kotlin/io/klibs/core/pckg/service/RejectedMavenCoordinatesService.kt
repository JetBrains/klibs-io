package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.entity.RejectedMavenCoordinateEntity
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.klibs.core.pckg.repository.RejectedMavenCoordinateRepository
import io.klibs.integration.maven.ScraperType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RejectedMavenCoordinatesService(
    private val rejectedMavenCoordinateRepository: RejectedMavenCoordinateRepository,
) {

    @Transactional
    fun save(
        mavenArtifact: MavenArtifactDTO,
        repo: ScraperType,
        scmUrl: String?,
        errorType: PackageIndexingErrorType,
    ) {
        rejectedMavenCoordinateRepository.save(
            RejectedMavenCoordinateEntity(
                mavenArtifact = mavenArtifact.toEntityRef(),
                repo = repo,
                scmUrl = scmUrl,
                errorType = errorType,
            )
        )
    }
}
