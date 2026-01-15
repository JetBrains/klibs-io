package io.klibs.core.project.blacklist

import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.project.repository.ProjectRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class BlacklistService(
    private val blacklistRepository: BlacklistRepository,
    private val packageRepository: PackageRepository,
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun banByGroup(groupId: String, reason: String?): Boolean {
        val projectIds = findProjectIdsByPackage(groupId, null)

        blacklistRepository.addToBannedPackages(groupId, null, reason)
        blacklistRepository.removeBannedPackages(groupId, null)
        blacklistRepository.removeBannedPackages()

        updateConnectedProjects(projectIds)

        return true
    }

    @Transactional
    fun banPackage(groupId: String, artifactId: String, reason: String?): Boolean {
        if (!blacklistRepository.checkPackageExists(groupId, artifactId)) {
            return false
        }

        if (blacklistRepository.checkPackageBanned(groupId, artifactId)) {
            return false
        }

        val projectIds = findProjectIdsByPackage(groupId, artifactId)

        blacklistRepository.addToBannedPackages(groupId, artifactId, reason)
        blacklistRepository.removeBannedPackages(groupId, artifactId)
        blacklistRepository.removeBannedPackages()

        updateConnectedProjects(projectIds)

        return true
    }

    private fun findProjectIdsByPackage(groupId: String, artifactId: String?): Set<Int> {
        return projectRepository.findProjectsByPackages(groupId, artifactId)
    }

    private fun updateConnectedProjects(projectIds: Set<Int>) {
        projectIds.forEach { projectId ->
            val latestPackages = packageRepository.findLatestByProjectId(projectId)

            if (latestPackages.isNotEmpty()) {
                val latestPackage = latestPackages.maxByOrNull { it.releaseTs }

                if (latestPackage != null) {
                    projectRepository.updateLatestVersion(
                        id = projectId,
                        latestVersion = latestPackage.version,
                        latestVersionTs = latestPackage.releaseTs
                    )
                }
            }
        }
    }
}
