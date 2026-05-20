package io.klibs.app.indexing

import io.klibs.app.util.ANDROIDX_OWNER_AND_GITHUB_REPOSITORY
import io.klibs.app.util.isAndroidxProject
import io.klibs.app.util.parseGitHubLink
import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.MavenArtifactService
import io.klibs.integration.maven.MavenPom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Owns everything derived from a package's POM during indexing: pure extractors for fields
 * consumed by [PackageIndexingService] (GitHub repo, developers, licenses) and persistence of
 * declared dependencies.
 *
 * Note on dependencies: only POM-declared dependencies are recorded — these correspond to the
 * "common" set in Kotlin Multiplatform libraries. Target-specific dependencies that appear only
 * in the Gradle module metadata are not captured.
 */
@Service
class PomIndexingService(
    private val packageRepository: PackageRepository,
    private val mavenArtifactService: MavenArtifactService,
) {

    /**
     * @return owner name to repo name, or null if the POM has no resolvable GitHub link.
     */
    fun extractGitHubRepoInfo(pom: MavenPom): Pair<String, String>? {
        val parsedGitHubLink = pom.scm?.url?.let(::parseGitHubLink) ?: pom.url?.let(::parseGitHubLink)
        if (parsedGitHubLink != null) return parsedGitHubLink

        val isAndroidx = pom.scm?.url?.isAndroidxProject() == true || pom.url?.isAndroidxProject() == true
        return if (isAndroidx) ANDROIDX_OWNER_AND_GITHUB_REPOSITORY else null
    }

    fun extractDevelopers(pom: MavenPom): List<PackageDeveloper> {
        val developers = pom.developers ?: return emptyList()
        return developers.mapNotNull { dev ->
            val name = (dev.name ?: dev.organization)
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            PackageDeveloper(
                name = name,
                url = dev.url ?: dev.email?.let { "mailto:$it" } ?: dev.organizationUrl
            )
        }
    }

    fun extractLicenses(pom: MavenPom): List<PackageLicense> {
        val licenses = pom.licenses ?: return emptyList()
        return licenses.mapNotNull { license ->
            val name = license.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PackageLicense(
                name = name,
                url = license.url
            )
        }
    }

    @Transactional
    fun indexDependencies(pom: MavenPom, packageId: Long, isReindex: Boolean) {
        val dependencies = pom.extractDependencies()

        if (!isReindex && dependencies.isEmpty()) return

        val packageEntity = requireNotNull(packageRepository.findById(packageId).orElse(null)) {
            "Package with id=$packageId not found while indexing dependencies"
        }

        if (isReindex) {
            packageEntity.dependencies.clear()
        }

        if (dependencies.isNotEmpty()) {
            val artifactsByCoords = mavenArtifactService.resolveOrCreateAll(dependencies)
            packageEntity.dependencies.addAll(artifactsByCoords.values.map { it.toEntityRef() })
        }

        packageRepository.save(packageEntity)
        logger.debug("Saved {} dependencies for package id={}", packageEntity.dependencies.size, packageId)
    }

    private fun MavenPom.extractDependencies(): Set<MavenCoordinatesDTO> =
        dependencies
            ?.asSequence()
            ?.mapNotNull { dep ->
                val group = dep.groupId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artifact = dep.artifactId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val version = dep.version?.takeIf { it.isNotBlank() && !it.contains("\${") }
                    ?: return@mapNotNull null
                MavenCoordinatesDTO(groupId = group, artifactId = artifact, version = version)
            }
            ?.filterNot { coords -> groupId == coords.groupId && artifactId == coords.artifactId }
            ?.toSet()
            .orEmpty()

    private companion object {
        private val logger = LoggerFactory.getLogger(PomIndexingService::class.java)
    }
}
