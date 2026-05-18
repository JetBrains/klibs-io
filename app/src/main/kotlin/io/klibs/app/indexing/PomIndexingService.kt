package io.klibs.app.indexing

import io.klibs.app.util.ANDROIDX_OWNER_AND_GITHUB_REPOSITORY
import io.klibs.app.util.isAndroidxProject
import io.klibs.app.util.parseGitHubLink
import io.klibs.core.pckg.entity.PackageDependencyEntity
import io.klibs.core.pckg.entity.PackageDependencyKey
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.repository.PackageDependencyRepository
import io.klibs.integration.maven.MavenPom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
    private val packageDependencyRepository: PackageDependencyRepository,
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

    fun indexDependencies(pom: MavenPom, packageId: Long, isReindex: Boolean) {
        val dependencies = pom.extractDependencies()

        if (isReindex) {
            packageDependencyRepository.deleteAllByIdPackageId(packageId)
        }

        if (dependencies.isEmpty()) return

        val entities = dependencies.map { coords ->
            PackageDependencyEntity(
                id = PackageDependencyKey(
                    packageId = packageId,
                    depGroupId = coords.groupId,
                    depArtifactId = coords.artifactId,
                    depVersion = coords.version,
                )
            )
        }
        packageDependencyRepository.saveAll(entities)
        logger.debug("Saved {} dependencies for package id={}", entities.size, packageId)
    }

    private fun MavenPom.extractDependencies(): Set<DependencyCoordinates> =
        dependencies
            ?.asSequence()
            ?.mapNotNull { dep ->
                val group = dep.groupId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artifact = dep.artifactId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val version = dep.version?.takeIf { it.isNotBlank() && !it.contains("\${") }
                    ?: return@mapNotNull null
                DependencyCoordinates(groupId = group, artifactId = artifact, version = version)
            }
            ?.filterNot { coords -> groupId == coords.groupId && artifactId == coords.artifactId }
            ?.toSet()
            .orEmpty()

    private data class DependencyCoordinates(
        val groupId: String,
        val artifactId: String,
        val version: String,
    )

    private companion object {
        private val logger = LoggerFactory.getLogger(PomIndexingService::class.java)
    }
}
