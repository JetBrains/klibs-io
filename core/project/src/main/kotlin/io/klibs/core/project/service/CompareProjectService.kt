package io.klibs.core.project.service

import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.ProjectDetails
import io.klibs.core.project.ProjectService
import io.klibs.core.project.api.CompareProjectRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompareProjectService(
    private val projectService: ProjectService,
    private val packageService: PackageService,
) {
    private companion object {
        private const val MAX_PROJECTS_TO_COMPARE = 10
        private const val MIN_PROJECTS_TO_COMPARE = 1
    }

    @Transactional(readOnly = true)
    fun compareProjects(projectReferences: List<CompareProjectRequest.ProjectReference>): Map<ProjectDetails, String?> {
        require(projectReferences.size <= MAX_PROJECTS_TO_COMPARE) {
            "Cannot compare more than $MAX_PROJECTS_TO_COMPARE and less than $MIN_PROJECTS_TO_COMPARE projects at once."
        }

        val projectsDetails =
            projectService.getProjectDetailsByNames(projectReferences.map { it.projectName to it.ownerLogin })

        if (projectsDetails.size != projectReferences.size) {
            val foundNames = projectsDetails.map { it.name }.toSet()
            val missingNames = projectReferences.map { it.projectName }.filter { it !in foundNames }
            throw IllegalArgumentException("Unable to find projects from this list: $missingNames")
        }

        val kotlinVersionsByProjectId =
            packageService.getKotlinVersionsByProjectIds(projectsDetails.map { it.id })

        return projectsDetails.associateWith { kotlinVersionsByProjectId[it.id] }
    }
}
