package io.klibs.app.indexing

import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerEntity
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.ProjectService
import io.klibs.core.project.enums.TagOrigin
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.scm.repository.readme.ReadmeService
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import io.klibs.integration.github.model.ReadmeFetchResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GitHubIndexingService(
    private val gitHubIntegration: GitHubIntegration,

    private val scmRepositoryRepository: ScmRepositoryRepository,
    private val scmOwnerRepository: ScmOwnerRepository,

    private val readmeService: ReadmeService,
    private val readmeContentBuilder: ReadmeContentBuilder,
    private val projectRepository: ProjectRepository,
    @Qualifier("ownerBackoffProvider")
    private val ownerBackoffProvider: BackoffProvider,
    private val projectService: ProjectService,
) {

    @Transactional
    fun syncOwnerWithGitHub() {
        var selectedOwnerId: Int? = null
        try {
            val ownerToUpdate = scmOwnerRepository.findForUpdate() ?: return

            if (ownerBackoffProvider.isBackedOff(ownerToUpdate.idNotNull)) {
                logger.debug("Selected ownerId={} login={} is in backoff; skipping this run", ownerToUpdate.id, ownerToUpdate.login)
                return
            }
            selectedOwnerId = ownerToUpdate.idNotNull

            val updated = updateOwner(ownerToUpdate)
            logger.debug("Updated GitHub owner: {}", updated)
            ownerBackoffProvider.onSuccess(ownerToUpdate.idNotNull)
        } catch (e: Exception) {
            logger.error("Error while updating a GitHub owner", e)
            selectedOwnerId?.let { ownerBackoffProvider.onFailure(it) }
        }
    }

    @Transactional
    fun updateRepo(repoToUpdate: ScmRepositoryEntity): ScmRepositoryEntity {
        val ghRepo =
            gitHubIntegration.getRepository(repoToUpdate.nativeId) ?: gitHubIntegration.getRepository(
                repoToUpdate.ownerLogin,
                repoToUpdate.name
            )
        if (ghRepo == null) {
            // TODO disable indexing at all, remove / hide the project
            scmRepositoryRepository.setUpdatedAt(repoToUpdate.idNotNull, Instant.now()).also { require(it) }
            logger.warn("Unable to find the GH repository for update: $repoToUpdate. Skipping it.")
            return repoToUpdate.copy(updatedAtTs = Instant.now())
        }

        val ownerId = updateRepositoryOwnerIfChanged(repoToUpdate, ghRepo)
        val projectEntity = projectRepository.findByNameAndScmRepoId(repoToUpdate.name, repoToUpdate.idNotNull)
            ?: error("Unable to find project entity for repoId=${repoToUpdate.idNotNull}")

        val hasReadme = updateReadme(projectEntity, ghRepo, repoToUpdate.updatedAtTs)
        val license = gitHubIntegration.getLicense(ghRepo.nativeId)

        val scmRepositoryEntity = repoToUpdate.copy(
            nativeId = ghRepo.nativeId,
            name = ghRepo.name,
            description = ghRepo.description,
            defaultBranch = ghRepo.defaultBranch,
            ownerId = ownerId,
            homepage = ghRepo.homepage,
            hasGhPages = ghRepo.hasGhPages,
            hasIssues = ghRepo.hasIssues,
            hasWiki = ghRepo.hasWiki,
            hasReadme = hasReadme,
            licenseKey = license?.key,
            licenseName = license?.name,
            stars = ghRepo.stars,
            openIssues = ghRepo.openIssues,
            lastActivityTs = ghRepo.lastActivity
        )

        logger.info("Updating ${ghRepo.owner}/${ghRepo.name}")
        val persistedRepo = if (scmRepositoryRepository.findByName(ghRepo.owner, ghRepo.name) != null) {
            scmRepositoryRepository.update(scmRepositoryEntity)
        } else {
            scmRepositoryRepository.upsert(scmRepositoryEntity)
        }

        // After repository is persisted, fetch and update GitHub tags for the linked project (if any)
        try {
            updateGithubTagsForProject(projectEntity, persistedRepo)
        } catch (e: Exception) {
            logger.error("Failed to update GitHub tags for repoId=${persistedRepo.idNotNull}", e)
        }

        return persistedRepo
    }

    /**
     * @return owner_id to set for the repo
     */
    private fun updateRepositoryOwnerIfChanged(repoToUpdate: ScmRepositoryEntity, ghRepo: GitHubRepository): Int {
        val hasChangedOwner = !repoToUpdate.ownerLogin.equals(ghRepo.owner, ignoreCase = true)
        if (!hasChangedOwner) {
            // small chance it was relocated to the owner of the same name?
            // rename original to tmp1, create new with the same name, move from tmp1 to the new one
            return repoToUpdate.ownerId
        }

        val ghUser = gitHubIntegration.getUser(ghRepo.owner)
            ?: error("Unable to find github user by login: ${ghRepo.owner}")

        val ownerEntity = scmOwnerRepository.findById(repoToUpdate.ownerId)
            ?: error("Unable to find owner entity by id: ${repoToUpdate.ownerId}")

        val relocatedToNewOwner = ghUser.id != ownerEntity.nativeId
        return if (relocatedToNewOwner) {
            indexOwner(ghUser).idNotNull
        } else {
            // this will get updated by the owner updater, it only needs the correct login
            scmOwnerRepository.updateLoginByNativeId(ownerEntity.nativeId, ghUser.login)
            ownerEntity.idNotNull
        }
    }

    /**
     * @return true if the repo has readme and it's been saved, false otherwise
     */
    private fun updateReadme(
        projectEntity: ProjectEntity,
        ghRepo: GitHubRepository,
        lastUpdatedTs: Instant
    ): Boolean {
        return when (val result = gitHubIntegration.getReadmeWithModifiedSinceCheck(
            ghRepo.nativeId,
            lastUpdatedTs
        )) {
            is ReadmeFetchResult.Content -> {
                val readmeContent = readmeContentBuilder.buildFromMarkdown(
                    readmeMd = result.markdown,
                    nativeId = ghRepo.nativeId,
                    ownerLogin = ghRepo.owner,
                    repoName = ghRepo.name,
                    defaultBranch = ghRepo.defaultBranch,
                )
                readmeService.writeReadmeFiles(
                    projectId = projectEntity.idNotNull,
                    mdContent = readmeContent.markdown,
                    htmlContent = readmeContent.html
                )
                projectEntity.minimizedReadme = readmeContent.minimized
                projectRepository.updateMinimizedReadme(projectEntity.idNotNull, readmeContent.minimized)
                true
            }
            is ReadmeFetchResult.NotModified -> {
                // README exists but hasn't changed; treat as having README
                true
            }
            is ReadmeFetchResult.NotFound -> false
            is ReadmeFetchResult.Error -> false
        }
    }

    private fun updateOwner(ownerEntity: ScmOwnerEntity): ScmOwnerEntity {
        val ghUser = gitHubIntegration.getUser(ownerEntity.login)
            ?: error("Unable to find a user with login ${ownerEntity.login}")

        return scmOwnerRepository.upsert(
            ownerEntity.copy(
                nativeId = ghUser.id,
                type = ghUser.getOwnerType(),
                name = ghUser.name,
                description = ghUser.bio,
                location = ghUser.location,
                followers = ghUser.followers,
                company = ghUser.company,
                homepage = buildOwnerHomepageLink(ghUser.blog),
                twitterHandle = ghUser.twitterUsername,
                email = ghUser.email,
                updatedAtTs = Instant.now()
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun indexRepository(ownerLogin: String, name: String): ScmRepositoryEntity? {
        val entity = scmRepositoryRepository.findByName(ownerLogin, name)
        if (entity != null) return entity

        logger.debug("Indexing a GitHub repository: $ownerLogin/$name")
        val repo = gitHubIntegration.getRepository(ownerLogin, name) ?: return null

        if (repo.hasBeenRelocated(originalOwnerLogin = ownerLogin, originalName = name)) {
            logger.debug("The indexed repository has been relocated: {}", repo)

            val relocatedEntity = scmRepositoryRepository.findByNativeId(repo.nativeId)
            if (relocatedEntity != null) return relocatedEntity
        }

        val ownerEntity = indexOwner(repo.owner)
        val license = gitHubIntegration.getLicense(repo.nativeId)

        val persistedEntity = scmRepositoryRepository.upsert(
            ScmRepositoryEntity(
                nativeId = repo.nativeId,
                name = repo.name,
                description = repo.description,
                defaultBranch = repo.defaultBranch,
                createdTs = repo.createdAt,
                ownerId = ownerEntity.idNotNull,
                ownerType = ownerEntity.type,
                ownerLogin = ownerEntity.login,
                homepage = repo.homepage,
                hasGhPages = repo.hasGhPages,
                hasIssues = repo.hasIssues,
                hasWiki = repo.hasWiki,
                hasReadme = false, // to be set later
                licenseKey = license?.key,
                licenseName = license?.name,
                stars = repo.stars,
                openIssues = repo.openIssues,
                lastActivityTs = repo.lastActivity,
                updatedAtTs = Instant.now()
            )
        )

        return persistedEntity
    }

    private fun GitHubRepository.hasBeenRelocated(originalOwnerLogin: String, originalName: String): Boolean {
        return !originalOwnerLogin.equals(this.owner, ignoreCase = true)
                || !originalName.equals(this.name, ignoreCase = true)
    }



    private fun indexOwner(ownerLogin: String): ScmOwnerEntity {
        val persistedEntity = scmOwnerRepository.findByLogin(ownerLogin)
        if (persistedEntity != null) return persistedEntity

        val ghUser = gitHubIntegration.getUser(ownerLogin)
            ?: error("Unable to find the following user: $ownerLogin")

        return indexOwner(ghUser)
    }

    private fun indexOwner(ghUser: GitHubUser): ScmOwnerEntity {
        return scmOwnerRepository.upsert(
            ScmOwnerEntity(
                id = null,
                nativeId = ghUser.id,
                type = ghUser.getOwnerType(),
                login = ghUser.login,
                name = ghUser.name,
                description = ghUser.bio,
                location = ghUser.location,
                followers = ghUser.followers,
                company = ghUser.company,
                homepage = buildOwnerHomepageLink(ghUser.blog),
                twitterHandle = ghUser.twitterUsername,
                email = ghUser.email,
                updatedAtTs = Instant.now()
            )
        )
    }

    private fun buildOwnerHomepageLink(homepage: String?): String? {
        val url = homepage ?: return null

        return when {
            url.startsWith("https://") -> url
            url.startsWith("http://") -> url.replaceFirst("http://", "https://")
            else -> "https://$url"
        }
    }

    private fun GitHubUser.getOwnerType(): ScmOwnerType {
        return when (this.type) {
            "User" -> ScmOwnerType.AUTHOR
            "Organization" -> ScmOwnerType.ORGANIZATION
            else -> error("Unknown user type: $type")
        }
    }

    private fun updateGithubTagsForProject(projectEntity: ProjectEntity?, scmRepositoryEntity: ScmRepositoryEntity?) {
        if (projectEntity == null || scmRepositoryEntity == null) return
        val repositoryTopics = gitHubIntegration.getRepositoryTopics(scmRepositoryEntity.nativeId)
        try {
            projectService.updateProjectTags(scmRepositoryEntity.name, scmRepositoryEntity.ownerLogin, repositoryTopics, TagOrigin.GITHUB)
        } catch (e: Exception) {
            logger.error("Failed to update GitHub tags for projectId=${projectEntity.idNotNull}", e)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GitHubIndexingService::class.java)
    }
}
