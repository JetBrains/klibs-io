package io.klibs.app.auth

import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.integration.github.GitHubIntegration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthorshipService(
    private val projectRepository: ProjectRepository,
    private val gitHubIntegration: GitHubIntegration
) {
    fun isAuthor(githubLogin: String, projectId: Int): Boolean {
        val owner = projectRepository.findOwnerByProjectId(projectId) ?: run {
            logger.warn("No owner found for projectId={}", projectId)
            return false
        }
        val result = when (owner.type) {
            ScmOwnerType.AUTHOR -> owner.login.equals(githubLogin, ignoreCase = true)
            ScmOwnerType.ORGANIZATION -> gitHubIntegration.isOrgMember(owner.login, githubLogin).also {
                logger.debug("Org membership check: login={} org={} result={}", githubLogin, owner.login, it)
            }
        }
        logger.debug("Authorship check: login={} projectId={} owner={} result={}", githubLogin, projectId, owner.login, result)
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthorshipService::class.java)
    }
}
