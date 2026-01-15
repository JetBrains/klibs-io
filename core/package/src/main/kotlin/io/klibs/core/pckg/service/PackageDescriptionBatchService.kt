package io.klibs.core.pckg.service

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.ai.PackageDescriptionGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class PackageDescriptionBatchService(
    private val packageRepository: PackageRepository,
    private val packageDescriptionGenerator: PackageDescriptionGenerator,
) {

    /**
     * Generates unique descriptions for a batch of packages that share the same description.
     * For each group of packages (grouped by groupId:artifactId), finds the latest version,
     * generates a new description using AI, and updates all versions of the package with the same description.
     *
     * @param description The shared description that needs to be replaced with unique ones
     * @param packages The list of packages that have the shared description
     * @return A map of package IDs to their new descriptions
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun generateUniqueDescriptionsForBatch(
        description: String,
        packages: List<PackageEntity>
    ): Map<Long, String> {
        val batchDescriptions = mutableMapOf<Long, String>()

        val packageGroups = packages.groupBy { "${it.groupId}:${it.artifactId}" }.filterValues { it.isNotEmpty() }

        for ((_, packagesInGroup) in packageGroups) {
            val latestPackage = packagesInGroup.maxBy { it.releaseTs }

            val newDescription = packageDescriptionGenerator.generatePackageDescription(
                latestPackage.name,
                latestPackage.groupId,
                latestPackage.artifactId,
                latestPackage.version
            )

            batchDescriptions[latestPackage.idNotNull] = newDescription

            val packagesToSave = mutableListOf<PackageEntity>()

            val allVersions = packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(
                latestPackage.groupId,
                latestPackage.artifactId
            ).filter { it.description == description }

            for (pkg in allVersions) {
                val updatedPackage = pkg.deepCopy(
                    description = newDescription,
                    generatedDescription = true
                )
                packagesToSave.add(updatedPackage)
                batchDescriptions[pkg.idNotNull] = newDescription
            }

            packageRepository.saveAll(packagesToSave)
        }

        return batchDescriptions
    }
}