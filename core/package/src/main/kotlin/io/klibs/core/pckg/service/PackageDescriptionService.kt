package io.klibs.core.pckg.service

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.repository.PackageIndexRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.integration.ai.PackageDescriptionGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PackageDescriptionService(
    private val packageRepository: PackageRepository,
    private val packageIndexRepository: PackageIndexRepository,
    private val packageDescriptionGenerator: PackageDescriptionGenerator,
    private val packageDescriptionBatchService: PackageDescriptionBatchService,
) {

    /**
     * Finds packages with duplicate descriptions, generates new unique descriptions for them using AI,
     * and updates the descriptions in the database for the latest version of each package.
     * Then applies the generated description to all other versions of the package which have the same description.
     *
     * @return A map of package IDs to their new descriptions
     */
    fun generateUniqueDescriptions() {
        logger.info("Starting processing duplicate descriptions")

        val newDescriptions = mutableMapOf<Long, String>()

        var duplicateDescriptions = packageRepository.findDuplicateDescriptions(limit = 10)

        if (duplicateDescriptions.isEmpty()) {
            logger.warn("No duplicate descriptions found to process")
            return
        }

        while (duplicateDescriptions.isNotEmpty()) {
            for (description in duplicateDescriptions) {
                val packages = packageRepository.findAllByDescription(description)
                val batchResults = packageDescriptionBatchService.generateUniqueDescriptionsForBatch(description, packages)
                newDescriptions.putAll(batchResults)
            }

            logger.info(
                """
                    Generated ${newDescriptions.size} unique descriptions for ${duplicateDescriptions.size} descriptions:
                    ${newDescriptions.entries.joinToString("\n") { "${it.key}: ${it.value}" }}
                """.trimIndent()
                    )

            newDescriptions.clear()

            duplicateDescriptions = packageRepository.findDuplicateDescriptions(limit = 10)
        }

        logger.info("Finished processing duplicate descriptions")
    }


    /**
     * Generate a description for a specific package identified by groupId, artifactId, and version.
     * If groupId, artifactId, and version are supplied: find package and update it using its name
     * If groupId and artifactId are supplied: update latest version of the package
     * If only groupId is supplied: update ALL latest versions of packages with corresponding groupId
     * If no packages were found, abort the operation
     *
     * @param groupId The group ID of the package
     * @param artifactId The artifact ID of the package (optional)
     * @param version The version of the package (optional)
     * @return The generated description or a map of package IDs to descriptions if multiple packages were updated
     * @throws IllegalArgumentException if no packages were found
     */
    @Transactional
    fun generateDescription(groupId: String, artifactId: String? = null, version: String? = null): String {
        // Case 1: groupId, artifactId, and version are all provided
        if (artifactId != null && version != null) {
            val packageEntity = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
                ?: throw IllegalArgumentException("No package found with groupId=$groupId, artifactId=$artifactId, version=$version")

            return generateDescriptionAndSave(packageEntity.groupId, packageEntity.artifactId, packageEntity.version, packageEntity)
        }

        // Case 2: groupId and artifactId are provided (get latest version)
        if (artifactId != null) {
            val packageEntity =
                packageRepository.findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId)
                    ?: throw IllegalArgumentException("No package found with groupId=$groupId, artifactId=$artifactId")

            return generateDescriptionAndSave(packageEntity.groupId, packageEntity.artifactId, packageEntity.version, packageEntity)
        }

        // Case 3: only groupId is provided (get all latest versions)
        val latestPackages = packageIndexRepository.findByIdGroupId(groupId)
        if (latestPackages.isEmpty()) {
            throw IllegalArgumentException("No packages found with groupId=$groupId")
        }

        val descriptions = mutableMapOf<String, String>()
        for (pkg in latestPackages) {
            val packageEntity = packageRepository.findById(pkg.latestPackageId)
                .orElseThrow { IllegalArgumentException("No package found with groupId=${pkg.id.groupId}, artifactId=${pkg.id.artifactId}")}

            val description = generateDescriptionAndSave(pkg.id.groupId, pkg.id.artifactId, pkg.latestVersion, packageEntity)
            descriptions["${pkg.id.groupId}:${pkg.id.artifactId}:${pkg.latestVersion}"] = description
        }

        val summary = buildString {
            append("Updated descriptions for ${descriptions.size} packages with groupId=$groupId:")
            descriptions.forEach { (packageId, description) ->
                append("\n$packageId: $description")
            }
        }

        logger.info(summary)

        // If only one package was updated, return just the description
        // Otherwise, return a summary of all updated descriptions
        return if (descriptions.size == 1) {
            descriptions.values.first()
        } else {
            summary
        }
    }

    /**
     * Generates the description of a package using AI and saves it to the database.
     *
     * @param groupId The group ID of the package
     * @param artifactId The artifact ID of the package
     * @param version The version of the package
     * @param packageEntity The package entity to update
     * @return The generated description
     */
    private fun generateDescriptionAndSave(
        groupId: String,
        artifactId: String,
        version: String,
        packageEntity: PackageEntity
    ): String{
        logger.info("Generating description for package: ${groupId}:${artifactId}:${version}")

        val description = packageDescriptionGenerator.generatePackageDescription(
            groupId,
            artifactId,
            version
        )

        val updatedPackage = packageEntity.deepCopy(
            description = description,
            generatedDescription = true
        )

        packageRepository.save(updatedPackage)

        logger.info("Generated description for package ${groupId}:${artifactId}:${version}: $description")

        return description
    }

    /**
     * Updates the description of a package with a user-provided description and saves it to the database.
     *
     * @param groupId The group ID of the package
     * @param artifactId The artifact ID of the package
     * @param version The version of the package
     * @param description The user-provided description
     * @return The updated description
     * @throws IllegalArgumentException if no package is found with the given coordinates
     */
    @Transactional
    fun updatePackageDescription(groupId: String, artifactId: String, version: String, description: String): String {
        val packageEntity = packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
            ?: throw IllegalArgumentException("No package found with groupId=$groupId, artifactId=$artifactId, version=$version")

        logger.info("Updating description for package: $groupId:$artifactId:$version")

        // Update the package description in the database and mark it as not generated (user-provided)
        val updatedPackage = packageEntity.deepCopy(
            description = description,
            generatedDescription = false
        )
        packageRepository.save(updatedPackage)

        logger.info("Updated description for package $groupId:$artifactId:$version: $description")

        return description
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PackageDescriptionService::class.java)
    }
}
