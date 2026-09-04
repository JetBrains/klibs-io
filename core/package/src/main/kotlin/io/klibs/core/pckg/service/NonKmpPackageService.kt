package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.entity.NonKmpPackageEntity
import io.klibs.core.pckg.repository.NonKmpPackageRepository
import io.klibs.integration.maven.ScraperType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NonKmpPackageService(
    private val nonKmpPackageRepository: NonKmpPackageRepository,
) {

    fun save(mavenArtifact: MavenArtifactDTO, releaseTs: Instant, repo: ScraperType, scmUrl: String?) {
        nonKmpPackageRepository.save(
            NonKmpPackageEntity(
                mavenArtifact = mavenArtifact.toEntityRef(),
                releaseTs = releaseTs,
                repo = repo,
                scmUrl = scmUrl,
            )
        )
    }
}
