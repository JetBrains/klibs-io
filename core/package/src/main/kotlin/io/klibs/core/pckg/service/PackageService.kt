package io.klibs.core.pckg.service

import io.klibs.core.pckg.entity.PackageEntity
import io.klibs.core.pckg.entity.PackageTargetEntity
import io.klibs.core.pckg.dto.PackageDTO
import io.klibs.core.pckg.model.PackageDetails
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackageOverview
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.repository.PackageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PackageService(
    private val packageRepository: PackageRepository
) {
    @Transactional(readOnly = false)
    fun updateByCoordinates(packageDTO: PackageDTO): PackageDTO? {
        val existingPackage = packageRepository.findByGroupIdAndArtifactIdAndVersion(
            packageDTO.groupId,
            packageDTO.artifactId,
            packageDTO.version
        ) ?: return null

        val updatedPackage = packageDTO.toEntity().deepCopy(id = existingPackage.id)

        val existingTargetsByKey = existingPackage.targets.associateBy { it.platform to it.target }

        updatedPackage.targets.clear()
        packageDTO.targets.forEach { incoming ->
            val key = incoming.platform to incoming.target
            val reused = existingTargetsByKey[key]
            if (reused != null) {
                updatedPackage.addTarget(reused)
            } else {
                updatedPackage.addTarget(
                    PackageTargetEntity(
                        platform = incoming.platform,
                        target = incoming.target
                    )
                )
            }
        }

        return PackageDTO.fromEntity(packageRepository.save(updatedPackage))
    }

    fun getPackageDetails(groupId: String, artifactId: String, version: String): PackageDetails? =
        packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)?.toModel()

    fun getLatestPackageDetails(groupId: String, artifactId: String): PackageDetails? =
        packageRepository.findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId)?.toModel()

    /**
     * @return **all** packages under the given [groupId] and [artifactId], meaning all versions
     */
    fun getPackages(groupId: String, artifactId: String): List<PackageOverview> =
        packageRepository.findByGroupIdAndArtifactIdOrderByReleaseTsDesc(groupId, artifactId).map { it.toOverview() }

    fun getLatestPackagesByGroupId(groupId: String): List<PackageOverview> =
        packageRepository.findLatestByGroupId(groupId).map { it.toOverview() }

    fun getLatestPackagesByProjectId(projectId: Int): List<PackageOverview> {
        return packageRepository.findLatestByProjectId(projectId).map { it.toOverview() }
    }
}


private fun PackageEntity.toModel(): PackageDetails {
    return PackageDetails(
        id = this.idNotNull,
        projectId = this.projectId,
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        releasedAt = this.releaseTs,
        name = this.name,
        description = this.description,
        targets = this.targets.map { PackageTarget(it.platform, it.target) },
        licenses = this.licenses.map { PackageLicense(it.name, it.url) },
        developers = this.developers.map { PackageDeveloper(it.name, it.url) },
        buildTool = this.buildTool,
        buildToolVersion = this.buildToolVersion,
        kotlinVersion = this.kotlinVersion,
        url = this.url,
        scmUrl = this.scmUrl
    )
}

private fun PackageEntity.toOverview(): PackageOverview {
    return PackageOverview(
        id = this.idNotNull,
        groupId = this.groupId,
        artifactId = this.artifactId,
        version = this.version,
        releasedAt = this.releaseTs,
        description = this.description,
        targets = this.targets.map { PackageTarget(it.platform, it.target) }
    )
}
