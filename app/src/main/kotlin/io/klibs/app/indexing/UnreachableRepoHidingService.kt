package io.klibs.app.indexing

import io.klibs.core.project.visibility.ProjectVisibilityService
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class UnreachableRepoHidingService(
    private val gitHubIntegration: GitHubIntegration,
    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val projectVisibilityService: ProjectVisibilityService,
    private val registry: MeterRegistry,

    @Value("\${klibs.integration.github.repo-unreachable-hide-after}")
    private val hideAfter: Duration,

    @Value("\${klibs.integration.github.repo-unreachable-max-corpus-share}")
    private val maxUnreachableCorpusShare: Double
) {

    init {
        Gauge.builder(UNREACHABLE_SHARE) { unreachableShareOrNaN() }
            .description("Klibs: Share of SCM repositories currently marked unreachable")
            .register(registry)
    }

    /**
     * Hides every project of [repo] once it has been unreachable for longer than the configured threshold.
     */
    fun hideIfUnreachableTooLong(repo: ScmRepositoryEntity) {
        val unreachableSince = repo.unreachableSince ?: return
        val unreachableFor = Duration.between(unreachableSince, Instant.now())
        if (unreachableFor < hideAfter) {
            return
        }

        if (!gitHubIsReachable() || !unreachableShareIsPlausible()) {
            return
        }

        val hidden = projectVisibilityService.hideAuto(
            scmRepoId = repo.idNotNull,
            reason = "GitHub repository unreachable since $unreachableSince"
        )
        if (hidden > 0) {
            logger.info(
                "Hid {} project(s) of {}/{}, unreachable since {}",
                hidden,
                repo.ownerLogin,
                repo.name,
                unreachableSince
            )
        }
    }

    /**
     * Un-hides the projects of a repository that is reachable again. Manual hides are left in place.
     */
    fun unhide(repo: ScmRepositoryEntity) {
        val unhidden = projectVisibilityService.unhideAuto(repo.idNotNull)
        if (unhidden > 0) {
            logger.info("Un-hid {} project(s) of {}/{}", unhidden, repo.ownerLogin, repo.name)
        }
    }

    /**
     * Never hide while blind: a broken GitHub path cannot tell a gone repository from an unanswered request.
     * `/rate_limit` costs no quota and fails on a rejected credential or an unreachable API.
     */
    private fun gitHubIsReachable(): Boolean =
        runCatching { gitHubIntegration.getRateLimitInfo() }
            .onFailure { logger.error("Refusing to hide projects: the GitHub API did not answer", it) }
            .isSuccess

    private fun unreachableShareIsPlausible(): Boolean {
        val share = scmRepositoryRepository.unreachableShare()
        if (share > maxUnreachableCorpusShare) {
            logger.error(
                "Refusing to hide projects: {}% of the repositories are unreachable, the limit is {}%",
                share * 100,
                maxUnreachableCorpusShare * 100
            )
            return false
        }
        return true
    }

    private fun unreachableShareOrNaN(): Double =
        runCatching { scmRepositoryRepository.unreachableShare() }
            .onFailure { logger.debug("Could not read the unreachable repository share", it) }
            .getOrDefault(Double.NaN)

    private companion object {
        private val logger = LoggerFactory.getLogger(UnreachableRepoHidingService::class.java)

        const val UNREACHABLE_SHARE = "klibs.scm.repo.unreachable.share"
    }
}
