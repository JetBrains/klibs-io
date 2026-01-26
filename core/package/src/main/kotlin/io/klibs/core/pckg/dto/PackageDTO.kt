package io.klibs.core.pckg.dto

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.entity.PackageTargetEntity
import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.integration.maven.ScraperType
import java.time.Instant

/**
 * Data Transfer Object for Package entity.
 * Used for transferring package data between layers.
 */
data class PackageDTO(
    val id: Long? = null,
    val projectId: Int?,
    val repo: ScraperType,
    val groupId: String,
    val artifactId: String,
    val version: String,
    val releaseTs: Instant,
    val description: String?,
    val url: String?,
    val scmUrl: String?,
    val buildTool: String,
    val buildToolVersion: String,
    val kotlinVersion: String,
    val developers: List<PackageDeveloper>,
    val licenses: List<PackageLicense>,
    val configuration: Configuration?,
    val generatedDescription: Boolean = false,
    val targets: List<PackageTarget> = emptyList()
) {
    /**
     * Converts this DTO to an entity.
     * @return PackageEntity created from this DTO
     */
    fun toEntity(): PackageEntity {
        val entity = PackageEntity(
            id = id,
            projectId = projectId,
            repo = repo,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            releaseTs = releaseTs,
            description = description,
            url = url,
            scmUrl = scmUrl,
            buildTool = buildTool,
            buildToolVersion = buildToolVersion,
            kotlinVersion = kotlinVersion,
            developers = developers,
            licenses = licenses,
            configuration = configuration,
            generatedDescription = generatedDescription
        )

        // Add targets to the entity
        targets.forEach { target ->
            val targetEntity = PackageTargetEntity(
                platform = target.platform,
                target = target.target
            )
            entity.addTarget(targetEntity)
        }

        return entity
    }

    companion object {
        /**
         * Creates a DTO from an entity.
         * @param entity The entity to convert
         * @return PackageDTO created from the entity
         */
        fun fromEntity(entity: PackageEntity): PackageDTO {
            return PackageDTO(
                id = entity.id,
                projectId = entity.projectId,
                repo = entity.repo,
                groupId = entity.groupId,
                artifactId = entity.artifactId,
                version = entity.version,
                releaseTs = entity.releaseTs,
                description = entity.description,
                url = entity.url,
                scmUrl = entity.scmUrl,
                buildTool = entity.buildTool,
                buildToolVersion = entity.buildToolVersion,
                kotlinVersion = entity.kotlinVersion,
                developers = entity.developers,
                licenses = entity.licenses,
                configuration = entity.configuration,
                generatedDescription = entity.generatedDescription,
                targets = entity.targets.map { PackageTarget(it.platform, it.target) }
            )
        }
    }
}
