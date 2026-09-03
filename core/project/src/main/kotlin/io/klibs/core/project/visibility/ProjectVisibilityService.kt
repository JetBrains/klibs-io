package io.klibs.core.project.visibility

import io.klibs.core.project.enums.HideOrigin
import io.klibs.core.project.repository.ProjectHiddenRepository
import io.klibs.core.project.repository.ProjectRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Hides and un-hides projects, the single writer of `project_hidden`.
 *
 * A hidden project keeps every row it has and keeps being indexed; it is only dropped from the read paths,
 * so that a project whose SCM repository comes back is immediately current again.
 */
@Service
class ProjectVisibilityService(
    private val projectHiddenRepository: ProjectHiddenRepository,
    private val projectRepository: ProjectRepository,
    private val registry: MeterRegistry
) {

    init {
        Gauge.builder(HIDDEN_PROJECTS) { hiddenCountOrNaN() }
            .description("Klibs: Number of currently hidden projects")
            .register(registry)
    }

    /**
     * Hides every project of an unreachable SCM repository. A repository can back several projects.
     *
     * @return number of projects hidden by this call
     */
    @Transactional
    fun hideAuto(scmRepoId: Int, reason: String?): Int {
        val at = Instant.now()
        val hidden = projectRepository.findIdsByScmRepoId(scmRepoId)
            .sumOf { projectHiddenRepository.hide(it, at, HideOrigin.AUTO, reason) }
        countHides(HideOrigin.AUTO, hidden)
        return hidden
    }

    /**
     * Un-hides the projects of an SCM repository that is reachable again. Manual hides are left in place.
     *
     * @return number of projects un-hidden by this call
     */
    @Transactional
    fun unhideAuto(scmRepoId: Int): Int {
        val projectIds = projectRepository.findIdsByScmRepoId(scmRepoId)
        if (projectIds.isEmpty()) {
            return 0
        }
        return projectHiddenRepository.deleteByProjectIdInAndOrigin(projectIds, HideOrigin.AUTO)
    }

    /**
     * Hides a project addressed the way an operator knows it.
     */
    @Transactional
    fun hideManual(ownerLogin: String, projectName: String, reason: String?): ProjectVisibilityChange {
        val projectId = findProjectId(ownerLogin, projectName) ?: return ProjectVisibilityChange.PROJECT_NOT_FOUND
        return changeOf(hideManual(projectId, reason))
    }

    /**
     * Un-hides a project addressed the way an operator knows it, regardless of how it was hidden.
     */
    @Transactional
    fun unhideManual(ownerLogin: String, projectName: String): ProjectVisibilityChange {
        val projectId = findProjectId(ownerLogin, projectName) ?: return ProjectVisibilityChange.PROJECT_NOT_FOUND
        return changeOf(unhideManual(projectId))
    }

    private fun hideManual(projectId: Int, reason: String?): Boolean {
        val hidden = projectHiddenRepository.hide(projectId, Instant.now(), HideOrigin.MANUAL, reason) > 0
        if (hidden) {
            logger.info("Hiding projectId={} manually, reason={}", projectId, reason)
            countHides(HideOrigin.MANUAL, 1)
        }
        return hidden
    }

    private fun unhideManual(projectId: Int): Boolean {
        val unhidden = projectHiddenRepository.deleteByProjectId(projectId) > 0
        if (unhidden) {
            logger.info("Un-hiding projectId={} manually", projectId)
        }
        return unhidden
    }

    private fun findProjectId(ownerLogin: String, projectName: String): Int? =
        projectRepository.findByNameAndOwnerLogin(projectName, ownerLogin)?.idNotNull

    private fun changeOf(changed: Boolean) =
        if (changed) ProjectVisibilityChange.CHANGED else ProjectVisibilityChange.ALREADY_IN_THAT_STATE

    private fun countHides(origin: HideOrigin, count: Int) {
        if (count <= 0) return
        Counter.builder(PROJECTS_HIDDEN)
            .description("Klibs: Number of projects hidden")
            .tag("origin", origin.name.lowercase())
            .register(registry)
            .increment(count.toDouble())
    }

    private fun hiddenCountOrNaN(): Double =
        runCatching { projectHiddenRepository.count().toDouble() }
            .onFailure { logger.debug("Could not count hidden projects", it) }
            .getOrDefault(Double.NaN)

    private companion object {
        private val logger = LoggerFactory.getLogger(ProjectVisibilityService::class.java)

        const val PROJECTS_HIDDEN = "klibs.project.hidden"
        const val HIDDEN_PROJECTS = "klibs.project.hidden.count"
    }
}
