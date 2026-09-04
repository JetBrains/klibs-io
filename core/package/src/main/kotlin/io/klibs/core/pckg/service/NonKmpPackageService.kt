package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.MavenArtifactDTO
import io.klibs.core.pckg.repository.NonKmpPackageRepository
import org.springframework.stereotype.Service

@Service
class NonKmpPackageService(
    private val nonKmpPackageRepository: NonKmpPackageRepository,
) {

    fun saveIfAbsent(mavenArtifact: MavenArtifactDTO) {
        nonKmpPackageRepository.saveIfAbsent(mavenArtifact.id)
    }
}
